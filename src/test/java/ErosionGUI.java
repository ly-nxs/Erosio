import com.github.keyboardcat1.erosio.Eroder;
import com.github.keyboardcat1.erosio.EroderGeometry;
import com.github.keyboardcat1.erosio.EroderResults;
import com.github.keyboardcat1.erosio.EroderSettings;
import com.github.keyboardcat1.erosio.geometries.EroderGeometryNatural;
import com.github.keyboardcat1.erosio.interpolation.Interpolator;
import com.github.keyboardcat1.erosio.interpolation.InterpolatorCPURasterizer;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import org.kynosarges.tektosyne.geometry.RectD;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;


public class ErosionGUI extends JFrame {

    private static final double WIDTH = 256e1;
    private static final int CONTROL_PANEL_WIDTH = 350;
    private static final int RENDER_SIZE = 600;
    private static final double SCALE =  512 / WIDTH;

    // Parameters
    private double uplift = 1.0;
    private double initialHeight = 0.0;
    private double erosionRate = 2.0;
    private double mnRatio = 0.5;
    private double maxSlope = 30.0;
    private double timeStep = 1.0;
    private int maxIterations = 10;
    private double convergenceThreshold = 1E-2;
    private int inverseSampleDensity = 2;
    private int seed = 2;
    // Add these with your other parameters
    private double waterLevel = 0.8;
    private float lightAngleX = 45.0f;
    private float lightAngleY = 45.0f;


    // UI Components
    private final Terrain3DPanel terrain3DPanel;
    private JProgressBar progressBar;
    private JButton generateButton;
    private JLabel statusLabel;
    private final JPanel heightmapPanel;
    private BufferedImage heightmapImage;

    // Slider labels
    private JLabel upliftValue;
    private JLabel initialHeightValue;
    private JLabel erosionRateValue;
    private JLabel mnRatioValue;
    private JLabel maxSlopeValue;
    private JLabel timeStepValue;
    private JLabel maxIterationsValue;
    private JLabel convergenceValue;
    private JLabel sampleDensityValue;
    private JLabel seedValue;
    private JLabel waterLevelValue;
    private JLabel lightAngleXValue;
    private JLabel lightAngleYValue;

    public ErosionGUI() {
        super("Erosion Simulator - 3D GPU Accelerated");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create control panel with scrolling
        JScrollPane scrollPane = new JScrollPane(createControlPanel());
        scrollPane.setPreferredSize(new Dimension(CONTROL_PANEL_WIDTH, RENDER_SIZE));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.WEST);

        // Create tabbed pane for different views
        JTabbedPane viewTabs = new JTabbedPane();

        // 3D view with GPU acceleration
        terrain3DPanel = new Terrain3DPanel();
        viewTabs.addTab("3D View (GPU)", terrain3DPanel);

        // Heightmap view
        heightmapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (heightmapImage != null) {
                    g.drawImage(heightmapImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.WHITE);
                    g.drawString("Click 'Generate' to create terrain", getWidth() / 2 - 100, getHeight() / 2);
                }
            }
        };
        heightmapPanel.setPreferredSize(new Dimension(RENDER_SIZE, RENDER_SIZE));
        heightmapPanel.setBackground(Color.BLACK);
        viewTabs.addTab("Heightmap", heightmapPanel);

        add(viewTabs, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel title = new JLabel("Erosion Parameters");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Uplift slider
        upliftValue = new JLabel(String.format("%.2f", uplift));
        panel.add(createSliderPanel("Uplift", 0, 500, (int) (uplift * 100), upliftValue, e -> {
            uplift = ((JSlider) e.getSource()).getValue() / 100.0;
            upliftValue.setText(String.format("%.2f", uplift));
        }));

        // Initial Height slider
        initialHeightValue = new JLabel(String.format("%.2f", initialHeight));
        panel.add(createSliderPanel("Initial Height", -100, 100, (int) (initialHeight * 10), initialHeightValue, e -> {
            initialHeight = ((JSlider) e.getSource()).getValue() / 10.0;
            initialHeightValue.setText(String.format("%.2f", initialHeight));
        }));

        // Erosion Rate slider
        erosionRateValue = new JLabel(String.format("%.2f", erosionRate));
        panel.add(createSliderPanel("Erosion Rate", 0, 1000, (int) (erosionRate * 100), erosionRateValue, e -> {
            erosionRate = ((JSlider) e.getSource()).getValue() / 100.0;
            erosionRateValue.setText(String.format("%.2f", erosionRate));
        }));

        // m:n Ratio slider
        mnRatioValue = new JLabel(String.format("%.2f", mnRatio));
        panel.add(createSliderPanel("m:n Ratio", 0, 100, (int) (mnRatio * 100), mnRatioValue, e -> {
            mnRatio = ((JSlider) e.getSource()).getValue() / 100.0;
            mnRatioValue.setText(String.format("%.2f", mnRatio));
        }));

        // Max Slope slider
        maxSlopeValue = new JLabel(String.format("%.1f", maxSlope));
        panel.add(createSliderPanel("Max Slope", 0, 100, (int) maxSlope, maxSlopeValue, e -> {
            maxSlope = ((JSlider) e.getSource()).getValue();
            maxSlopeValue.setText(String.format("%.1f", maxSlope));
        }));

        // Time Step slider
        timeStepValue = new JLabel(String.format("%.2f", timeStep));
        panel.add(createSliderPanel("Time Step", 1, 1000, (int) (timeStep * 100), timeStepValue, e -> {
            timeStep = ((JSlider) e.getSource()).getValue() / 100.0;
            timeStepValue.setText(String.format("%.2f", timeStep));
        }));

        // Max Iterations slider
        maxIterationsValue = new JLabel(String.format("%d", maxIterations));
        panel.add(createSliderPanel("Max Iterations", 1, 100, maxIterations, maxIterationsValue, e -> {
            maxIterations = ((JSlider) e.getSource()).getValue();
            maxIterationsValue.setText(String.format("%d", maxIterations));
        }));

        // Convergence Threshold slider
        convergenceValue = new JLabel(String.format("%.2e", convergenceThreshold));
        panel.add(createSliderPanel("Convergence Threshold", -6, 0, -2, convergenceValue, e -> {
            convergenceThreshold = Math.pow(10, ((JSlider) e.getSource()).getValue());
            convergenceValue.setText(String.format("%.2e", convergenceThreshold));
        }));

        // Inverse Sample Density slider
        sampleDensityValue = new JLabel(String.format("%d", inverseSampleDensity));
        panel.add(createSliderPanel("Inverse Sample Density", 1, 500, inverseSampleDensity, sampleDensityValue, e -> {
            inverseSampleDensity = ((JSlider) e.getSource()).getValue();
            sampleDensityValue.setText(String.format("%d", inverseSampleDensity));
        }));

        // Seed slider
        seedValue = new JLabel(String.format("%d", seed));
        panel.add(createSliderPanel("Seed", 1, 100, seed, seedValue, e -> {
            seed = ((JSlider) e.getSource()).getValue();
            seedValue.setText(String.format("%d", seed));
        }));

        // Add after the seed slider and before the progress bar

// Water Level slider
        waterLevelValue = new JLabel(String.format("%.2f", waterLevel));
        panel.add(createSliderPanel("Water Level", 0, 100, (int) (waterLevel * 100), waterLevelValue, e -> {
            waterLevel = ((JSlider) e.getSource()).getValue() / 100.0;
            waterLevelValue.setText(String.format("%.2f", waterLevel));
            terrain3DPanel.setWaterLevel((float) waterLevel);
        }));

// Light Angle X slider
        lightAngleXValue = new JLabel(String.format("%.1f°", lightAngleX));
        panel.add(createSliderPanel("Light Angle X", -90, 90, (int) lightAngleX, lightAngleXValue, e -> {
            lightAngleX = ((JSlider) e.getSource()).getValue();
            lightAngleXValue.setText(String.format("%.1f°", lightAngleX));
            terrain3DPanel.setLightAngleX(lightAngleX);
        }));

// Light Angle Y slider
        lightAngleYValue = new JLabel(String.format("%.1f°", lightAngleY));
        panel.add(createSliderPanel("Light Angle Y", 0, 360, (int) lightAngleY, lightAngleYValue, e -> {
            lightAngleY = ((JSlider) e.getSource()).getValue();
            lightAngleYValue.setText(String.format("%.1f°", lightAngleY));
            terrain3DPanel.setLightAngleY(lightAngleY);
        }));

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(300, 25));
        panel.add(progressBar);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(statusLabel);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Generate button
        generateButton = new JButton("Generate Terrain");
        generateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateButton.setPreferredSize(new Dimension(200, 40));
        generateButton.setMaximumSize(new Dimension(200, 40));
        generateButton.addActionListener(e -> generateTerrain());
        panel.add(generateButton);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Reset button
        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.setPreferredSize(new Dimension(200, 30));
        resetButton.setMaximumSize(new Dimension(200, 30));
        resetButton.addActionListener(e -> resetDefaults());
        panel.add(resetButton);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // 3D Controls info
        JTextArea controlsInfo = getJTextArea(panel);
        panel.add(controlsInfo);

        return panel;
    }

    private static JTextArea getJTextArea(JPanel panel) {
        JTextArea controlsInfo = new JTextArea(
                """
                        3D View Controls:
                        • Click + Drag: Rotate
                        • Mouse Wheel: Zoom
                        • Right-click: Reset view
                        • GPU accelerated with
                          Phong shading"""
        );
        controlsInfo.setEditable(false);
        controlsInfo.setBackground(panel.getBackground());
        controlsInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        return controlsInfo;
    }

    private JPanel createSliderPanel(String label, int min, int max, int initial, JLabel valueLabel, ChangeListener listener) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(CONTROL_PANEL_WIDTH - 40, 80));

        JPanel labelPanel = new JPanel(new BorderLayout());
        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        labelPanel.add(nameLabel, BorderLayout.WEST);
        labelPanel.add(valueLabel, BorderLayout.EAST);
        panel.add(labelPanel);

        JSlider slider = new JSlider(min, max, initial);
        slider.addChangeListener(listener);
        panel.add(slider);

        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        return panel;
    }

    private void resetDefaults() {
        uplift = 1.0;
        initialHeight = 0.0;
        erosionRate = 2.0;
        mnRatio = 0.5;
        maxSlope = 30.0;
        timeStep = 1.0;
        maxIterations = 10;
        convergenceThreshold = 1E-2;
        inverseSampleDensity = 2;
        seed = 2;
        waterLevel = 0.8;
        lightAngleX = 45.0f;
        lightAngleY = 45.0f;

        upliftValue.setText(String.format("%.2f", uplift));
        initialHeightValue.setText(String.format("%.2f", initialHeight));
        erosionRateValue.setText(String.format("%.2f", erosionRate));
        mnRatioValue.setText(String.format("%.2f", mnRatio));
        maxSlopeValue.setText(String.format("%.1f", maxSlope));
        timeStepValue.setText(String.format("%.2f", timeStep));
        maxIterationsValue.setText(String.format("%d", maxIterations));
        convergenceValue.setText(String.format("%.2e", convergenceThreshold));
        sampleDensityValue.setText(String.format("%d", inverseSampleDensity));
        seedValue.setText(String.format("%d", seed));
        waterLevelValue.setText(String.format("%.2f", waterLevel));
        lightAngleXValue.setText(String.format("%.1f°", lightAngleX));
        lightAngleYValue.setText(String.format("%.1f°", lightAngleY));

        terrain3DPanel.setWaterLevel((float) waterLevel);
        terrain3DPanel.setLightAngleX(lightAngleX);
        terrain3DPanel.setLightAngleY(lightAngleY);

        statusLabel.setText("Reset to defaults");
    }

    private void generateTerrain() {
        generateButton.setEnabled(false);
        statusLabel.setText("Generating...");
        progressBar.setValue(0);

        SwingWorker<TerrainData, Integer> worker = new SwingWorker<>() {
            @Override
            protected TerrainData doInBackground() {
                // Large world bounds
                RectD bounds = new RectD(-WIDTH, -WIDTH, WIDTH, WIDTH);

                EroderSettings settings = new EroderSettings(
                        (p, t) -> uplift,
                        p -> initialHeight,
                        p -> erosionRate,
                        mnRatio,
                        (p, h) -> maxSlope,
                        timeStep,
                        maxIterations,
                        convergenceThreshold
                );

                publish(20);

                // Use coarser sampling for large areas
                int adjustedSampleDensity = Math.max(inverseSampleDensity, (int) (WIDTH / 512));

                EroderGeometry eroderGeometry = new EroderGeometryNatural(
                        EroderGeometry.RectDtoPolygon(bounds),
                        adjustedSampleDensity,
                        seed
                );

                publish(40);

                EroderResults results = Eroder.erode(settings, eroderGeometry);

                publish(60);

                System.out.printf("World size: %.0f x %.0f\n", WIDTH, WIDTH);
                System.out.printf("Texture resolution: %d x %d\n", 512, 512);
                System.out.printf("Node count: %d\nConverged: %d\nHeight range: %.2f -> %.2f\n",
                        eroderGeometry.nodeCount(), results.converged, results.minHeight, results.maxHeight);

                // Use CPU rasterizer for better performance on large areas
                Interpolator interpolator = new InterpolatorCPURasterizer(results, 10, 0);

                publish(70);

                // Generate at texture resolution, not world size
                double[][] heightData = new double[512][512];
                BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);

                double cellSize = WIDTH / 512;

                for (int x = 0; x < 512; x++) {
                    for (int y = 0; y < 512; y++) {
                        // Map texture coordinates to world coordinates
                        double worldX = (x * cellSize) - WIDTH / 2;
                        double worldY = (y * cellSize) - WIDTH / 2;

                        double value = interpolator.interpolate(worldX, worldY) - results.minHeight;
                        heightData[x][y] = value / (results.maxHeight - results.minHeight);

                        int intensity = (int) (255 * heightData[x][y]);
                        intensity = Math.max(0, Math.min(255, intensity));
                        image.setRGB(x, y, new Color(intensity, intensity, intensity).getRGB());
                    }

                    if (x % 10 == 0) {
                        publish(70 + (int) (30.0 * x / 512));
                    }
                }

                publish(100);

                return new TerrainData(heightData, image);
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                progressBar.setValue(chunks.getLast());
            }

            @Override
            protected void done() {
                try {
                    TerrainData data = get();
                    heightmapImage = data.heightmap;
                    terrain3DPanel.setHeightData(data.heightData);
                    heightmapPanel.repaint();
                    statusLabel.setText(String.format("Generated %.0f x %.0f world at %dx%d resolution",
                            WIDTH, WIDTH, 512, 512));
                    progressBar.setValue(100);
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                    e.printStackTrace();
                }
                generateButton.setEnabled(true);
            }
        };

        worker.execute();
    }




    static class TerrainData {
        double[][] heightData;
        BufferedImage heightmap;

        TerrainData(double[][] heightData, BufferedImage heightmap) {
            this.heightData = heightData;
            this.heightmap = heightmap;
        }
    }

    static class Terrain3DPanel extends JPanel implements GLEventListener {
        private final FPSAnimator animator;
        private double[][] heightData;
        private float rotationX = 45;
        private float rotationY = 45;
        private float zoom = 2.5f;
        private int lastMouseX, lastMouseY;
        private float waterLevel = 0.8f;
        private float lightAngleX = 45.0f;
        private float lightAngleY = 45.0f;

        private FloatBuffer vertexBuffer;
        private FloatBuffer normalBuffer;
        private FloatBuffer colorBuffer;
        private IntBuffer indexBuffer;

        public Terrain3DPanel() {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(RENDER_SIZE, RENDER_SIZE));

            GLProfile profile = GLProfile.get(GLProfile.GL2);
            GLCapabilities capabilities = new GLCapabilities(profile);
            capabilities.setDepthBits(24);
            capabilities.setDoubleBuffered(true);
            capabilities.setHardwareAccelerated(true);

            GLCanvas canvas = new GLCanvas(capabilities);
            canvas.addGLEventListener(this);

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();

                    if (e.getButton() == MouseEvent.BUTTON3) {
                        rotationX = 45;
                        rotationY = 45;
                        zoom = 2.5f;
                    }
                }
            };

            MouseMotionAdapter motionAdapter = new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        int dx = e.getX() - lastMouseX;
                        int dy = e.getY() - lastMouseY;

                        rotationY += dx * 0.5f;
                        rotationX += dy * 0.5f;
                        rotationX = Math.max(-90, Math.min(90, rotationX));

                        lastMouseX = e.getX();
                        lastMouseY = e.getY();
                    }
                }
            };

            canvas.addMouseListener(mouseAdapter);
            canvas.addMouseMotionListener(motionAdapter);
            canvas.addMouseWheelListener(e -> {
                zoom *= (float) Math.pow(0.9, e.getWheelRotation());
                zoom = Math.max(0.5f, Math.min(10.0f, zoom));
            });

            add(canvas, BorderLayout.CENTER);

            animator = new FPSAnimator(canvas, 60);
            animator.start();
        }

        public void setHeightData(double[][] heightData) {
            this.heightData = heightData;
            generateMesh();
        }

        private void generateMesh() {
            if (heightData == null) return;

            int size = heightData.length;
            // Adaptive LOD based on texture size
            int targetGridSize = Math.min(size, 150); // Max 150x150 grid for performance
            int step = Math.max(1, size / targetGridSize);
            int gridSize = size / step;

            System.out.printf("Mesh: %dx%d grid from %dx%d texture (step=%d)\n",
                    gridSize, gridSize, size, size, step);

            int vertexCount = gridSize * gridSize;
            float[] vertices = new float[vertexCount * 3];
            float[] normals = new float[vertexCount * 3];
            float[] colors = new float[vertexCount * 3];
            int[] indices = new int[(gridSize - 1) * (gridSize - 1) * 6];

            // Generate vertices, normals, and colors
            for (int z = 0; z < gridSize; z++) {
                for (int x = 0; x < gridSize; x++) {
                    int idx = (z * gridSize + x) * 3;
                    int heightX = Math.min(x * step, size - 1);
                    int heightZ = Math.min(z * step, size - 1);

                    float height = (float) heightData[heightX][heightZ];

                    vertices[idx] = (x - gridSize / 2.0f) / (gridSize / 2.0f);
                    vertices[idx + 1] = height * 0.5f;
                    vertices[idx + 2] = (z - gridSize / 2.0f) / (gridSize / 2.0f);

                    // Calculate normal using surrounding points
                    float[] normal = calculateNormal(x, z, gridSize, step, size);
                    normals[idx] = normal[0];
                    normals[idx + 1] = normal[1];
                    normals[idx + 2] = normal[2];

                    // Color based on height
                    float[] color = getHeightColor(height);
                    colors[idx] = color[0];
                    colors[idx + 1] = color[1];
                    colors[idx + 2] = color[2];
                }
            }

            // Generate indices for triangles
            int indexPos = 0;
            for (int z = 0; z < gridSize - 1; z++) {
                for (int x = 0; x < gridSize - 1; x++) {
                    int topLeft = z * gridSize + x;
                    int topRight = topLeft + 1;
                    int bottomLeft = (z + 1) * gridSize + x;
                    int bottomRight = bottomLeft + 1;

                    indices[indexPos++] = topLeft;
                    indices[indexPos++] = bottomLeft;
                    indices[indexPos++] = topRight;

                    indices[indexPos++] = topRight;
                    indices[indexPos++] = bottomLeft;
                    indices[indexPos++] = bottomRight;
                }
            }

            vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.flip();

            normalBuffer = ByteBuffer.allocateDirect(normals.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            normalBuffer.put(normals);
            normalBuffer.flip();

            colorBuffer = ByteBuffer.allocateDirect(colors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            colorBuffer.put(colors);
            colorBuffer.flip();

            indexBuffer = ByteBuffer.allocateDirect(indices.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
            indexBuffer.put(indices);
            indexBuffer.flip();
        }

        private float[] calculateNormal(int x, int z, int gridSize, int step, int size) {
            float height = (float) heightData[Math.min(x * step, size - 1)][Math.min(z * step, size - 1)];

            float heightL = x > 0 ? (float) heightData[Math.min((x - 1) * step, size - 1)][Math.min(z * step, size - 1)] : height;
            float heightR = x < gridSize - 1 ? (float) heightData[Math.min((x + 1) * step, size - 1)][Math.min(z * step, size - 1)] : height;
            float heightD = z > 0 ? (float) heightData[Math.min(x * step, size - 1)][Math.min((z - 1) * step, size - 1)] : height;
            float heightU = z < gridSize - 1 ? (float) heightData[Math.min(x * step, size - 1)][Math.min((z + 1) * step, size - 1)] : height;

            float nx = (heightL - heightR) * 2.0f;
            float ny = 0.1f;
            float nz = (heightD - heightU) * 2.0f;

            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0) {
                nx /= length;
                ny /= length;
                nz /= length;
            }

            return new float[]{nx, ny, nz};
        }

        private float[] getHeightColor(float height) {
            if (height < 0.2f) {
                return new float[]{0.13f, 0.55f, 0.13f}; // Dark green
            } else if (height < 0.4f) {
                return new float[]{0.42f, 0.56f, 0.14f}; // Olive green
            } else if (height < 0.6f) {
                return new float[]{0.55f, 0.35f, 0.17f}; // Brown
            } else if (height < 0.8f) {
                return new float[]{0.63f, 0.63f, 0.63f}; // Gray
            } else {
                return new float[]{0.94f, 0.94f, 1.0f}; // Snow-white
            }
        }

        @Override
        public void init(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();

            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glDepthFunc(GL2.GL_LEQUAL);
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_LIGHT0);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            gl.glColorMaterial(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE);
            gl.glEnable(GL2.GL_NORMALIZE);
            gl.glShadeModel(GL2.GL_SMOOTH);

            // Setup lighting
            float[] lightPos = {1.0f, 1.0f, 1.0f, 0.0f};
            float[] lightAmbient = {0.3f, 0.3f, 0.35f, 1.0f};
            float[] lightDiffuse = {0.8f, 0.8f, 0.8f, 1.0f};
            float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f};

            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, lightAmbient, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDiffuse, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightSpecular, 0);

            // Material properties
            float[] matSpecular = {0.3f, 0.3f, 0.3f, 1.0f};
            float[] matShininess = {32.0f};
            gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, matSpecular, 0);
            gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SHININESS, matShininess, 0);

            gl.glClearColor(0.08f, 0.08f, 0.16f, 1.0f);
        }

        @Override
        public void display(GLAutoDrawable drawable) {
            GL2 gl = getGl2(drawable);

            // Draw terrain
            if (vertexBuffer != null && indexBuffer != null) {
                gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
                gl.glEnableClientState(GL2.GL_COLOR_ARRAY);

                gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vertexBuffer);
                gl.glNormalPointer(GL2.GL_FLOAT, 0, normalBuffer);
                gl.glColorPointer(3, GL2.GL_FLOAT, 0, colorBuffer);

                gl.glDrawElements(GL2.GL_TRIANGLES, indexBuffer.capacity(), GL2.GL_UNSIGNED_INT, indexBuffer);

                gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
                gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
                gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
            }

            // Draw water plane
            drawWater(gl);

            gl.glFlush();
        }

        private GL2 getGl2(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();

            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
            gl.glLoadIdentity();

            gl.glTranslatef(0, 0, -zoom);
            gl.glRotatef(rotationX, 1, 0, 0);
            gl.glRotatef(rotationY, 0, 1, 0);

            // Update light position based on angles
            float lightX = (float) (Math.cos(Math.toRadians(lightAngleY)) * Math.cos(Math.toRadians(lightAngleX)));
            float lightY = (float) Math.sin(Math.toRadians(lightAngleX));
            float lightZ = (float) (Math.sin(Math.toRadians(lightAngleY)) * Math.cos(Math.toRadians(lightAngleX)));
            float[] lightPos = {lightX, lightY, lightZ, 0.0f};
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
            return gl;
        }

        private void drawWater(GL2 gl) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_BLEND);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

            // Scale water height to match terrain
            float waterHeight = (waterLevel - 0.5f) * 0.5f;

            gl.glColor4f(0.2f, 0.4f, 0.7f, 0.6f); // Semi-transparent blue
            gl.glBegin(GL2.GL_QUADS);
            gl.glVertex3f(-1.5f, waterHeight, -1.5f);
            gl.glVertex3f(1.5f, waterHeight, -1.5f);
            gl.glVertex3f(1.5f, waterHeight, 1.5f);
            gl.glVertex3f(-1.5f, waterHeight, 1.5f);
            gl.glEnd();

            gl.glDisable(GL2.GL_BLEND);
            gl.glEnable(GL2.GL_LIGHTING);
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            GL2 gl = drawable.getGL().getGL2();

            height = Math.max(height, 1);
            float aspect = (float) width / height;

            gl.glViewport(0, 0, width, height);
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();

            float fovy = 45.0f;
            float zNear = 0.1f;
            float zFar = 100.0f;
            float fH = (float) Math.tan(fovy / 360.0 * Math.PI) * zNear;
            float fW = fH * aspect;
            gl.glFrustum(-fW, fW, -fH, fH, zNear, zFar);

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();
        }

        @Override
        public void dispose(GLAutoDrawable drawable) {
            if (animator != null) {
                animator.stop();
            }
        }

        public void setWaterLevel(float level) {
            this.waterLevel = level;
        }

        public void setLightAngleX(float angle) {
            this.lightAngleX = angle;
        }

        public void setLightAngleY(float angle) {
            this.lightAngleY = angle;
        }
    }

    public static void main(String[] args) {
        // Set system property to fix JOGL module access issues
        System.setProperty("jogl.disable.openglarbcontext", "true");

        SwingUtilities.invokeLater(ErosionGUI::new);
    }
}
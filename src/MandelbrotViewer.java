import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * MandelbrotViewer.java
 *
 * Enhanced Swing‐based Mandelbrot/Julia viewer:
 *  • Smooth coloring (continuous iteration count)
 *  • Click-to-zoom in (left-click) or zoom out (right-click)
 *  • Press 'J' to toggle Mandelbrot <-> Julia mode.  When switching to Julia,
 *    the complex constant c is picked from your current mouse position.
 *  • Press 'S' to save the current view as a PNG (timestamped).
 *  • 'Reset View' button always available to go back to the original coordinates.
 */
public class MandelbrotViewer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MandelbrotFrame::new);
    }
}

/**
 * MandelbrotFrame holds the MandelbrotPanel and a small toolbar.
 */
class MandelbrotFrame extends JFrame {
    public MandelbrotFrame() {
        setTitle("Mandelbrot/Julia Viewer (Press J to toggle, S to save)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MandelbrotPanel panel = new MandelbrotPanel(800, 600);
        JButton resetBtn = new JButton("Reset View");
        resetBtn.addActionListener(e -> panel.resetView());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(resetBtn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(panel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}

/**
 * MandelbrotPanel
 *
 *  • Renders Mandelbrot or Julia set into a BufferedImage with smooth coloring.
 *  • Left-click zooms in; right-click zooms out.
 *  • 'J' toggles between Mandelbrot and Julia (Julia c is captured from mouse).
 *  • 'S' saves the current image as a timestamped PNG.
 */
class MandelbrotPanel extends JPanel {
    private final int width, height;
    private BufferedImage image;

    // Original “world” bounds (for Reset)
    private final double ORIG_REAL_MIN = -2.0;
    private final double ORIG_REAL_MAX =  1.0;
    private final double ORIG_IMAG_MIN = -1.2;
    private final double ORIG_IMAG_MAX =  1.2;

    // Current “world” bounds
    private double realMin = ORIG_REAL_MIN;
    private double realMax = ORIG_REAL_MAX;
    private double imagMin = ORIG_IMAG_MIN;
    private double imagMax = ORIG_IMAG_MAX;

    private static final int MAX_ITER       = 800;
    private static final double ESCAPE_RAD  = 2.0;

    // Julia‐mode fields
    private boolean isJulia = false;
    private double juliaCRe = 0.0;
    private double juliaCIm = 0.0;

    // Track last known mouse position (for picking Julia c)
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public MandelbrotPanel(int width, int height) {
        this.width = width;
        this.height = height;
        setPreferredSize(new Dimension(width, height));

        // Render initial Mandelbrot
        renderFractal();

        // MouseMotionListener tracks cursor so we can pick Julia c when 'J' is pressed
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        // MouseListener for zoom in/out
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Map click to complex-plane coordinates:
                double clickRe = realMin + e.getX() * (realMax - realMin) / (width - 1);
                double clickIm = imagMax - e.getY() * (imagMax - imagMin) / (height - 1);

                // Left-click => zoom in (factor 0.5); Right-click => zoom out (factor 2.0)
                double factor = (e.getButton() == MouseEvent.BUTTON3) ? 2.0 : 0.5;

                double realRange = (realMax - realMin) * factor / 2;
                double imagRange = (imagMax - imagMin) * factor / 2;

                realMin = clickRe - realRange;
                realMax = clickRe + realRange;
                imagMin = clickIm - imagRange;
                imagMax = clickIm + imagRange;

                renderFractal();
                repaint();
            }
        });

        // KeyListener for 'J' (toggle Mandelbrot<->Julia) and 'S' (save PNG)
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char c = Character.toLowerCase(e.getKeyChar());
                if (c == 'j') {
                    // If switching to Julia, capture juliaC from last mouse position:
                    if (!isJulia) {
                        juliaCRe = realMin + lastMouseX * (realMax - realMin) / (width - 1);
                        juliaCIm = imagMax - lastMouseY * (imagMax - imagMin) / (height - 1);
                        isJulia = true;
                    } else {
                        isJulia = false; // go back to Mandelbrot
                    }
                    renderFractal();
                    repaint();
                }
                else if (c == 's') {
                    saveImageToDisk();
                }
            }
        });

        // Allow the panel to receive focus so KeyListener will work
        setFocusable(true);
        requestFocusInWindow();
    }

    /** Reset view back to the original Mandelbrot rectangle (and force Mandelbrot mode). */
    public void resetView() {
        realMin = ORIG_REAL_MIN;
        realMax = ORIG_REAL_MAX;
        imagMin = ORIG_IMAG_MIN;
        imagMax = ORIG_IMAG_MAX;
        isJulia = false;
        renderFractal();
        repaint();
    }

    /** Main rendering entry point: decides Mandelbrot vs. Julia. */
    private void renderFractal() {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        double realStep = (realMax - realMin) / (width - 1);
        double imagStep = (imagMax - imagMin) / (height - 1);

        for (int y = 0; y < height; y++) {
            double cImLine = imagMax - y * imagStep;
            for (int x = 0; x < width; x++) {
                double cReLine = realMin + x * realStep;

                double iterValue;
                if (!isJulia) {
                    // Mandelbrot: z₀ = 0, c = (cReLine, cImLine)
                    iterValue = continuousIterationMandelbrot(cReLine, cImLine);
                } else {
                    // Julia: z₀ = (cReLine, cImLine), c = (juliaCRe, juliaCIm)
                    iterValue = continuousIterationJulia(cReLine, cImLine, juliaCRe, juliaCIm);
                }

                int color = getSmoothColor(iterValue);
                image.setRGB(x, y, color);
            }
        }
    }

    /**
     * Compute a “smooth” iteration count for Mandelbrot:
     *  z₀ = (0, 0),  c = (cRe, cIm).
     *  Return a fractional iteration count:
     *    n + 1 − log(log|zₙ|)/log(2)
     */
    private double continuousIterationMandelbrot(double cRe, double cIm) {
        double zRe = 0.0, zIm = 0.0;
        double zReSq = 0.0, zImSq = 0.0;
        int iter = 0;

        while (zReSq + zImSq <= ESCAPE_RAD * ESCAPE_RAD && iter < MAX_ITER) {
            double tempIm = 2 * zRe * zIm + cIm;
            double tempRe = zReSq - zImSq + cRe;
            zRe = tempRe;
            zIm = tempIm;
            zReSq = zRe * zRe;
            zImSq = zIm * zIm;
            iter++;
        }
        if (iter >= MAX_ITER) {
            return MAX_ITER;
        }
        double modZ = Math.sqrt(zReSq + zImSq);
        // “Smooth” iteration:
        return iter + 1 - Math.log(Math.log(modZ)) / Math.log(2);
    }

    /**
     * Compute a “smooth” iteration count for Julia:
     *  z₀ = (z0Re, z0Im),  c = (cRe, cIm).
     *  Return n + 1 − log(log|zₙ|)/log(2).
     */
    private double continuousIterationJulia(double z0Re, double z0Im, double cRe, double cIm) {
        double zRe = z0Re, zIm = z0Im;
        double zReSq = zRe * zRe, zImSq = zIm * zIm;
        int iter = 0;

        while (zReSq + zImSq <= ESCAPE_RAD * ESCAPE_RAD && iter < MAX_ITER) {
            double tempIm = 2 * zRe * zIm + cIm;
            double tempRe = zReSq - zImSq + cRe;
            zRe = tempRe;
            zIm = tempIm;
            zReSq = zRe * zRe;
            zImSq = zIm * zIm;
            iter++;
        }
        if (iter >= MAX_ITER) {
            return MAX_ITER;
        }
        double modZ = Math.sqrt(zReSq + zImSq);
        return iter + 1 - Math.log(Math.log(modZ)) / Math.log(2);
    }

    /**
     * Map a “smooth” iteration value to an RGB color using HSB:
     *  • If iter ≥ MAX_ITER → inside set → black.
     *  • Else:
     *      hue = 0.95 − 0.95*(iter/MAX_ITER)    (0.95=reddish → 0.0=purple)
     *      sat = 0.6 + 0.4*(iter/MAX_ITER)     (0.6→1.0)
     *      bri = 1.0
     */
    private int getSmoothColor(double iterSmooth) {
        if (iterSmooth >= MAX_ITER) {
            return Color.BLACK.getRGB();
        }
        float frac = (float)(iterSmooth / MAX_ITER);
        float hue = (float)(0.95 - 0.95 * frac);
        float sat = 0.6f + 0.4f * frac;
        float bri = 1.0f;
        return Color.HSBtoRGB(hue, sat, bri);
    }

    /**
     * Save the current BufferedImage to a PNG with a timestamped filename.
     * e.g. “mandelbrot_20250602T231529.png”
     */
    private void saveImageToDisk() {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
            String timestamp = LocalDateTime.now().format(fmt);
            String mode = isJulia ? "julia" : "mandelbrot";
            String filename = "fractal_" + mode + "_" + timestamp + ".png";

            File outFile = new File(filename);
            ImageIO.write(image, "png", outFile);
            System.out.println("Saved current view to: " + filename);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            int xoff = (getWidth()  - width)  / 2;
            int yoff = (getHeight() - height) / 2;
            g.drawImage(image, xoff, yoff, null);

            // Draw text overlay in upper-left corner: mode + instructions
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String modeText = isJulia
                    ? String.format("Julia (c = %.4f + %.4fi)  [J=toggle  S=save]", juliaCRe, juliaCIm)
                    : "Mandelbrot  [J=toggle  S=save]";
            g.drawString(modeText, xoff + 5, yoff + 20);
        }
    }
}

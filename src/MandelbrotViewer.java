import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * MandelbrotViewer.java
 *
 * A simple Swing‐based Mandelbrot set viewer with colored rendering and click‐to‐zoom.
 */
public class MandelbrotViewer {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            new MandelbrotFrame();
        });
    }
}

/**
 * JFrame that holds the MandelbrotPanel.
 */
class MandelbrotFrame extends JFrame {
    public MandelbrotFrame() {
        setTitle("Mandelbrot Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MandelbrotPanel panel = new MandelbrotPanel(800, 600);
        getContentPane().add(panel);
        pack();
        setLocationRelativeTo(null); // center on screen
        setVisible(true);
    }
}

/**
 * MandelbrotPanel
 *
 * - Renders the Mandelbrot set into a BufferedImage.
 * - Supports click‐to‐zoom: each click recenters at the clicked point and halves the view range.
 */
class MandelbrotPanel extends JPanel {
    private final int width;
    private final int height;
    private BufferedImage image;

    // Complex‐plane bounds:
    private double realMin = -2.0;
    private double realMax = 1.0;
    private double imagMin = -1.2;
    private double imagMax = 1.2;

    private static final int MAX_ITER = 500;

    public MandelbrotPanel(int width, int height) {
        this.width = width;
        this.height = height;
        setPreferredSize(new Dimension(width, height));

        renderMandelbrot();

        // Mouse listener for click‐to‐zoom:
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Compute new center based on click:
                double clickReal = realMin + e.getX() * (realMax - realMin) / (width - 1);
                double clickImag = imagMax - e.getY() * (imagMax - imagMin) / (height - 1);

                // Halve the range for zoom
                double realRange = (realMax - realMin) / 4; // /2 on each side
                double imagRange = (imagMax - imagMin) / 4;

                realMin = clickReal - realRange;
                realMax = clickReal + realRange;
                imagMin = clickImag - imagRange;
                imagMax = clickImag + imagRange;

                renderMandelbrot();
                repaint();
            }
        });
    }

    /**
     * Iterates z_{n+1} = z_n^2 + c, returns the iteration count (up to MAX_ITER).
     */
    private int mandelbrotIterations(double cRe, double cIm) {
        double zRe = 0.0;
        double zIm = 0.0;
        int iter = 0;

        while (zRe * zRe + zIm * zIm <= 4.0 && iter < MAX_ITER) {
            double nextRe = zRe * zRe - zIm * zIm + cRe;
            double nextIm = 2 * zRe * zIm + cIm;
            zRe = nextRe;
            zIm = nextIm;
            iter++;
        }
        return iter;
    }

    /**
     * Renders the entire Mandelbrot set into the BufferedImage.
     */
    private void renderMandelbrot() {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        double realStep = (realMax - realMin) / (width - 1);
        double imagStep = (imagMax - imagMin) / (height - 1);

        for (int y = 0; y < height; y++) {
            double cIm = imagMax - y * imagStep;
            for (int x = 0; x < width; x++) {
                double cRe = realMin + x * realStep;
                int iter = mandelbrotIterations(cRe, cIm);

                int color = getColor(iter);
                image.setRGB(x, y, color);
            }
        }
    }

    /**
     * Maps an iteration count to an RGB color using HSB for smooth gradients.
     * Points inside the set (iter == MAX_ITER) are colored black.
     */
    private int getColor(int iter) {
        if (iter >= MAX_ITER) {
            return Color.BLACK.getRGB();
        }
        // hue: [0.0 .. 1.0), by mapping iter to a fraction
        float hue = (float) iter / MAX_ITER;
        float saturation = 1.0f;
        float brightness = iter < MAX_ITER ? 1.0f : 0.0f;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, null);
        }
    }
}

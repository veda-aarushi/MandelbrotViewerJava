# MandelbrotViewerJava

A Swing-based interactive fractal viewer.  
**Now with:**
- **Mandelbrot & Julia modes** (press **J** to toggle).
- **Click-to-zoom** (left-click to zoom in, right-click to zoom out).
- **Save as PNG** (press **S** to save a timestamped screenshot).
- **Reset View** button to restore the original coordinates.

---

## Features

1. **Mandelbrot Mode**
    - Renders \(\displaystyle z_{n+1} = z_n^2 + c\) for \(z_0=0\).
    - Smooth coloring (no banding) by computing a continuous iteration count:
      \[
      \nu = n + 1 - \frac{\log(\log|z_n|)}{\log 2}\quad\text{when }|z_n|>2.
      \]
    - Color mapping uses an HSB gradient from red → purple → black.

2. **Julia Mode**
    - Press **J** to switch into Julia.
    - In Julia mode, the complex constant \(c\) is “picked” from your mouse’s last position on the fractal window when you pressed **J**.
    - To return to Mandelbrot, just press **J** again.

3. **Interactive Zoom**
    - **Left-click** anywhere in the fractal to zoom in by a factor of 2 (recentered on that point).
    - **Right-click** to zoom out by a factor of 2.
    - Both Mandelbrot and Julia respond identically to zoom/pan.

4. **Save as PNG**
    - Press **S** at any time to dump the current view to `fractal_mandelbrot_yyyyMMddTHHmmss.png`  
      or `fractal_julia_yyyyMMddTHHmmss.png` (timestamped).
    - Saves directly in the project’s working directory.

5. **Reset View**
    - A handy **Reset View** button (top-left) immediately restores:
        - Mandelbrot mode
        - original bounds \([-2.0,1.0]\times[-1.2,1.2]\).

---

## Prerequisites

- **Java 8 or newer** (tested with Java 11+).
- (Optional) Any IDE (IntelliJ, Eclipse) or just command line.

---

## Build & Run

1. **Compile**
   ```bash
   javac -d bin src/MandelbrotViewer.java

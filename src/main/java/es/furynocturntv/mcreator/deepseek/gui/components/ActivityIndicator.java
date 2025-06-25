package es.furynocturntv.mcreator.deepseek.gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

public class ActivityIndicator extends JComponent {
    private float angle = 0;
    private Timer timer;
    private Color[] gradientColors;
    private int arcLength = 90;
    private boolean running = false;

    public ActivityIndicator() {
        setPreferredSize(new Dimension(24, 24));
        setMinimumSize(new Dimension(16, 16));
        gradientColors = new Color[] {
                new Color(65, 131, 215),
                new Color(45, 111, 195),
                new Color(25, 91, 175)
        };

        // Configurar animación
        timer = new Timer(16, e -> {
            angle = (angle + 2) % 360;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight());
        int strokeWidth = size / 8;
        int padding = strokeWidth / 2;

        // Fondo circular
        Ellipse2D background = new Ellipse2D.Double(
                padding, padding,
                size - strokeWidth, size - strokeWidth
        );

        g2.setColor(new Color(240, 240, 240));
        g2.fill(background);

        // Arco animado
        Arc2D arc = new Arc2D.Double(
                padding, padding,
                size - strokeWidth, size - strokeWidth,
                angle, arcLength,
                Arc2D.OPEN
        );

        // Crear gradiente para el arco
        float[] fractions = {0.0f, 0.5f, 1.0f};
        RadialGradientPaint gradient = new RadialGradientPaint(
                new Point(size/2, size/2), size/2, fractions, gradientColors
        );

        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(gradient);
        g2.draw(arc);

        g2.dispose();
    }

    public void start() {
        if (!running) {
            running = true;
            timer.start();
        }
    }

    public void stop() {
        if (running) {
            running = false;
            timer.stop();
            repaint();
        }
    }

    public void setArcLength(int length) {
        this.arcLength = Math.max(30, Math.min(120, length));
    }

    public void setGradientColors(Color... colors) {
        this.gradientColors = colors.length > 0 ? colors : this.gradientColors;
    }

    public boolean isRunning() {
        return running;
    }
}
package simekiri;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class DegradeImageApp extends JFrame {
    private BufferedImage originalImage;
    private BufferedImage degradedImage;

    private JLabel originalLabel = new JLabel();
    private JLabel degradedLabel = new JLabel();

    private static final int PREVIEW_WIDTH = 400;
    private static final int PREVIEW_HEIGHT = 400;

    private JSlider noiseSlider;
    private JSlider quantizeSlider;
    private JSlider shuffleSlider;
    private JSlider glitchSlider;
    private JSlider blockifySlider;

    private JButton saveButton;
    private Dimension initialWindowSize;
    private File originalFile;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        SwingUtilities.invokeLater(DegradeImageApp::new);
    }

    public DegradeImageApp() {
        setTitle("画像劣化比較アプリ（各方式個別調整）");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 840);
        initialWindowSize = getSize();
        setLayout(new BorderLayout());

        JButton themeButton = new JButton("テーマ変更");
        themeButton.addActionListener(e -> new ThemeDialog(this));
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(themeButton);
        add(topPanel, BorderLayout.NORTH);

        JScrollPane originalScroll = new JScrollPane(originalLabel);
        JScrollPane degradedScroll = new JScrollPane(degradedLabel);
        originalScroll.setBorder(BorderFactory.createTitledBorder("元画像"));
        degradedScroll.setBorder(BorderFactory.createTitledBorder("劣化画像"));

        JPanel imagePanel = new JPanel(new GridLayout(1, 2));
        imagePanel.add(originalScroll);
        imagePanel.add(degradedScroll);
        add(imagePanel, BorderLayout.CENTER);

        noiseSlider = createSlider();
        quantizeSlider = createSlider();
        shuffleSlider = createSlider();
        glitchSlider = createSlider();
        blockifySlider = createSlider();

        saveButton = new JButton("保存");
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> {
            if (degradedImage == null) return;

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("保存先を選択");
            fileChooser.setSelectedFile(new File("degraded.png"));

            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    ImageIO.write(degradedImage, "png", file);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        JPanel sliderPanel = new JPanel(new GridLayout(6, 1));
        sliderPanel.add(createLabeledSlider("ノイズ強度:", noiseSlider));
        sliderPanel.add(createLabeledSlider("減色強度:", quantizeSlider));
        sliderPanel.add(createLabeledSlider("シャッフル強度:", shuffleSlider));
        sliderPanel.add(createLabeledSlider("グリッチ強度:", glitchSlider));
        sliderPanel.add(createLabeledSlider("ブロック強度:", blockifySlider));
        sliderPanel.add(saveButton);

        add(sliderPanel, BorderLayout.SOUTH);

        // ラベルの画像を中央寄せ
        originalLabel.setHorizontalAlignment(JLabel.CENTER);
        degradedLabel.setHorizontalAlignment(JLabel.CENTER);

        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        loadImage(files.get(0));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        ChangeListener updateListener = (ChangeEvent e) -> updateDegradedImage();
        noiseSlider.addChangeListener(updateListener);
        quantizeSlider.addChangeListener(updateListener);
        shuffleSlider.addChangeListener(updateListener);
        glitchSlider.addChangeListener(updateListener);
        blockifySlider.addChangeListener(updateListener);

        setVisible(true);
    }

    private JPanel createLabeledSlider(String labelText, JSlider slider) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(100, 20)); // ラベル幅を統一
        panel.add(label, BorderLayout.WEST);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    private JSlider createSlider() {
        JSlider slider = new JSlider(0, 1000, 0);
        slider.setMajorTickSpacing(250);
        slider.setMinorTickSpacing(50);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        // 軽量化：スライダーがドラッグ終了時にのみ処理実行
        slider.addChangeListener(e -> {
            if (!slider.getValueIsAdjusting()) {
                updateDegradedImage();
            }
        });
        return slider;
    }
    
    class ThemeDialog extends JDialog {
        public ThemeDialog(JFrame parent) {
            super(parent, "テーマ選択", true);
            setSize(300, 120);
            setLayout(new FlowLayout());

            String[] themes = {"Dark", "Light"};
            JComboBox<String> selector = new JComboBox<>(themes);
            selector.setSelectedItem("Dark");

            JButton apply = new JButton("適用");
            apply.addActionListener(e -> {
                String selected = (String) selector.getSelectedItem();
                try {
                    switch (selected) {
                    case "Light": UIManager.setLookAndFeel(new FlatLightLaf()); break;
                    case "Dark": UIManager.setLookAndFeel(new FlatDarkLaf()); break;
                    }
                    SwingUtilities.updateComponentTreeUI(parent);
                    parent.setSize(initialWindowSize);
                    dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            add(new JLabel("テーマ:"));
            add(selector);
            add(apply);

            setLocationRelativeTo(parent);
            setVisible(true);
        }
    }

    private void loadImage(File file) throws IOException {
        originalFile = file;
        originalImage = ImageIO.read(file);
        updateDegradedImage();
        saveButton.setEnabled(true);
    }

    private void updateDegradedImage() {
        if (originalImage == null) return;

        BufferedImage img = originalImage;
        if (noiseSlider.getValue() > 0)
            img = applyNoise(img, noiseSlider.getValue());
        if (quantizeSlider.getValue() > 0)
            img = applyQuantize(img, quantizeSlider.getValue());
        if (shuffleSlider.getValue() > 0)
            img = applyShuffle(img, shuffleSlider.getValue());
        if (glitchSlider.getValue() > 0)
            img = applyGlitch(img, glitchSlider.getValue());
        if (blockifySlider.getValue() > 0)
            img = applyBlockify(img, blockifySlider.getValue());

        degradedImage = img;

        originalLabel.setIcon(new ImageIcon(scaleImage(originalImage, PREVIEW_WIDTH, PREVIEW_HEIGHT)));
        degradedLabel.setIcon(new ImageIcon(scaleImage(degradedImage, PREVIEW_WIDTH, PREVIEW_HEIGHT)));
    }

    //劣化処理
    private BufferedImage applyNoise(BufferedImage img, int strength) {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Random rand = new Random();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y));
                int r = clamp(c.getRed() + rand.nextInt(strength + 1) - strength / 2);
                int g = clamp(c.getGreen() + rand.nextInt(strength + 1) - strength / 2);
                int b = clamp(c.getBlue() + rand.nextInt(strength + 1) - strength / 2);
                out.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return out;
    }

    private BufferedImage applyQuantize(BufferedImage img, int strength) {
        int levels = Math.max(2, 256 / Math.max(1, strength / 20));
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y));
                int r = (c.getRed() * levels / 256) * (256 / levels);
                int g = (c.getGreen() * levels / 256) * (256 / levels);
                int b = (c.getBlue() * levels / 256) * (256 / levels);
                out.setRGB(x, y, new Color(clamp(r), clamp(g), clamp(b)).getRGB());
            }
        }
        return out;
    }

    private BufferedImage applyShuffle(BufferedImage img, int strength) {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        int w = img.getWidth(), h = img.getHeight();
        int total = w * h;
        int[] pixels = new int[total];

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                pixels[y * w + x] = img.getRGB(x, y);

        Random rand = new Random();
        int swaps = strength * total / 1000;

        for (int i = 0; i < swaps; i++) {
            int a = rand.nextInt(total);
            int b = rand.nextInt(total);
            int temp = pixels[a];
            pixels[a] = pixels[b];
            pixels[b] = temp;
        }

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(x, y, pixels[y * w + x]);

        return out;
    }

    private BufferedImage applyGlitch(BufferedImage img, int strength) {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Random rand = new Random();
        for (int y = 0; y < img.getHeight(); y++) {
            int shift = rand.nextInt((strength / 20) + 1) - (strength / 40);
            for (int x = 0; x < img.getWidth(); x++) {
                int sx = clampIndex(x + shift, img.getWidth());
                out.setRGB(x, y, img.getRGB(sx, y));
            }
        }
        return out;
    }

    private BufferedImage applyBlockify(BufferedImage img, int strength) {
        int blockSize = Math.max(2, strength / 16 + 2);
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < img.getHeight(); y += blockSize) {
            for (int x = 0; x < img.getWidth(); x += blockSize) {
                int r = 0, g = 0, b = 0, count = 0;
                for (int dy = 0; dy < blockSize; dy++) {
                    for (int dx = 0; dx < blockSize; dx++) {
                        int px = x + dx, py = y + dy;
                        if (px < img.getWidth() && py < img.getHeight()) {
                            Color c = new Color(img.getRGB(px, py));
                            r += c.getRed();
                            g += c.getGreen();
                            b += c.getBlue();
                            count++;
                        }
                    }
                }
                Color avg = new Color(r / count, g / count, b / count);
                for (int dy = 0; dy < blockSize; dy++) {
                    for (int dx = 0; dx < blockSize; dx++) {
                        int px = x + dx, py = y + dy;
                        if (px < img.getWidth() && py < img.getHeight()) {
                            out.setRGB(px, py, avg.getRGB());
                        }
                    }
                }
            }
        }
        return out;
    }

    private int clamp(int val) {
        return Math.min(255, Math.max(0, val));
    }

    private int clampIndex(int i, int max) {
        return Math.min(max - 1, Math.max(0, i));
    }

    private Image scaleImage(BufferedImage img, int maxWidth, int maxHeight) {
        int width = img.getWidth();
        int height = img.getHeight();
        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
        if (scale >= 1.0f) return img;
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        return img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    }
}

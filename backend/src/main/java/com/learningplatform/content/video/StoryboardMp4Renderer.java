package com.learningplatform.content.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders storyboard scenes into a real H.264/AVC MP4 (slide video) using jcodec.
 */
@Component
public class StoryboardMp4Renderer {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS = 2;

    private final ObjectMapper objectMapper;

    public StoryboardMp4Renderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] renderMp4(String videoJson) {
        try {
            JsonNode root = objectMapper.readTree(videoJson);
            String title = root.path("title").asText("Lesson");
            List<Scene> scenes = new ArrayList<>();
            for (JsonNode n : root.path("scenes")) {
                scenes.add(new Scene(
                        n.path("onScreenText").asText(title),
                        n.path("narration").asText(""),
                        Math.max(2, n.path("durationSeconds").asInt(8))
                ));
            }
            if (scenes.isEmpty()) {
                scenes.add(new Scene(title, "No scenes available.", 4));
            }
            return encode(title, scenes);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to render MP4: " + ex.getMessage(), ex);
        }
    }

    private byte[] encode(String courseHint, List<Scene> scenes) throws Exception {
        Path temp = Files.createTempFile("alp-lesson-", ".mp4");
        try {
            SeekableByteChannel out = NIOUtils.writableChannel(temp.toFile());
            AWTSequenceEncoder encoder = new AWTSequenceEncoder(out, Rational.R(FPS, 1));
            for (int i = 0; i < scenes.size(); i++) {
                Scene scene = scenes.get(i);
                BufferedImage frame = drawFrame(courseHint, scene, i + 1, scenes.size());
                int frameCount = Math.max(FPS * 2, Math.min(scene.durationSeconds(), 4) * FPS);
                for (int f = 0; f < frameCount; f++) {
                    encoder.encodeImage(frame);
                }
            }
            encoder.finish();
            out.close();
            return Files.readAllBytes(temp);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static BufferedImage drawFrame(String courseHint, Scene scene, int index, int total) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(12, 28, 22));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(new Color(61, 155, 110, 80));
            g.fillOval(-120, -160, 520, 420);
            g.setColor(new Color(40, 90, 140, 60));
            g.fillOval(860, 360, 560, 480);

            g.setColor(new Color(155, 181, 168));
            g.setFont(new Font("SansSerif", Font.PLAIN, 28));
            g.drawString(courseHint, 64, 64);
            g.drawString("Scene " + index + " / " + total, WIDTH - 220, 64);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 54));
            drawWrapped(g, scene.onScreenText(), 64, 220, WIDTH - 128, 64);

            g.setColor(new Color(210, 230, 220));
            g.setFont(new Font("SansSerif", Font.PLAIN, 32));
            drawWrapped(g, scene.narration(), 64, 400, WIDTH - 128, 40);

            g.setColor(new Color(61, 155, 110));
            int barW = (int) ((WIDTH - 128) * (index / (double) total));
            g.fillRoundRect(64, HEIGHT - 56, barW, 12, 8, 8);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void drawWrapped(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text == null || text.isBlank()) {
            return;
        }
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int cy = y;
        int lines = 0;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                g.drawString(line.toString(), x, cy);
                line = new StringBuilder(word);
                cy += lineHeight;
                lines++;
                if (lines >= 5) {
                    g.drawString("…", x, cy);
                    return;
                }
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            g.drawString(line.toString(), x, cy);
        }
    }

    private record Scene(String onScreenText, String narration, int durationSeconds) {
    }
}

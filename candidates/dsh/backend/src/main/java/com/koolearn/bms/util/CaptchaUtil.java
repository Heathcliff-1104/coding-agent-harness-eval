package com.koolearn.bms.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaUtil {

    private static final ConcurrentHashMap<String, CaptchaEntry> STORE = new ConcurrentHashMap<>();
    private static final long EXPIRE_MS = 5 * 60 * 1000;
    private static final SecureRandom RAND = new SecureRandom();

    static {
        Thread cleanup = new Thread(() -> {
            while (true) {
                try { Thread.sleep(60000); } catch (InterruptedException e) { break; }
                long now = System.currentTimeMillis();
                STORE.entrySet().removeIf(e -> now > e.getValue().expireAt);
            }
        });
        cleanup.setDaemon(true);
        cleanup.start();
    }

    public static CaptchaResult generate() {
        String code = randomCode(4);
        String key = UUID.randomUUID().toString().replace("-", "");
        BufferedImage img = drawImage(code);
        String base64;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("生成验证码失败", e);
        }
        STORE.put(key, new CaptchaEntry(code, System.currentTimeMillis() + EXPIRE_MS));
        return new CaptchaResult(key, base64);
    }

    public static boolean verify(String key, String code) {
        if (key == null || code == null) return false;
        CaptchaEntry entry = STORE.remove(key);
        if (entry == null) return false;
        return entry.code.equalsIgnoreCase(code);
    }

    private static String randomCode(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(RAND.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static BufferedImage drawImage(String code) {
        int w = 120, h = 44;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, w, h);
        g.setFont(new Font("Arial", Font.BOLD, 28));
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(180 + RAND.nextInt(60), 180 + RAND.nextInt(60), 180 + RAND.nextInt(60)));
            g.drawLine(RAND.nextInt(w), RAND.nextInt(h), RAND.nextInt(w), RAND.nextInt(h));
        }
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(30 + RAND.nextInt(80), 30 + RAND.nextInt(80), 30 + RAND.nextInt(80)));
            int x = 10 + i * 26 + RAND.nextInt(6);
            int y = 30 + RAND.nextInt(8);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }
        g.dispose();
        return img;
    }

    public static class CaptchaResult {
        public String key;
        public String image;
        public CaptchaResult(String key, String image) { this.key = key; this.image = image; }
    }

    private static class CaptchaEntry {
        String code;
        long expireAt;
        CaptchaEntry(String code, long expireAt) { this.code = code; this.expireAt = expireAt; }
    }
}

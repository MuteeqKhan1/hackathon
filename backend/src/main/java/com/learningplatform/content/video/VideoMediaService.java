package com.learningplatform.content.video;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningplatform.source.storage.ObjectStorage;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class VideoMediaService {

    private final StoryboardMp4Renderer renderer;
    private final ObjectStorage objectStorage;
    private final ObjectMapper objectMapper;

    public VideoMediaService(
            StoryboardMp4Renderer renderer,
            ObjectStorage objectStorage,
            ObjectMapper objectMapper
    ) {
        this.renderer = renderer;
        this.objectStorage = objectStorage;
        this.objectMapper = objectMapper;
    }

    /**
     * Renders MP4 from storyboard JSON, stores it, and returns content JSON with mp4ObjectKey set.
     */
    public String renderAndAttach(String videoJson, String storageKey) {
        byte[] mp4 = renderer.renderMp4(videoJson);
        objectStorage.put(storageKey, new ByteArrayInputStream(mp4), mp4.length, "video/mp4");
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(videoJson);
            root.put("mp4ObjectKey", storageKey);
            root.put("mp4ContentType", "video/mp4");
            root.put("mp4Bytes", mp4.length);
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to attach mp4 metadata", ex);
        }
    }

    public byte[] loadOrRender(String storageKey, String videoJson) {
        if (objectStorage.exists(storageKey)) {
            return read(storageKey);
        }
        byte[] mp4 = renderer.renderMp4(videoJson);
        objectStorage.put(storageKey, new ByteArrayInputStream(mp4), mp4.length, "video/mp4");
        return mp4;
    }

    /**
     * Prefer an already-rendered file (personalized or generation-time key) before encoding again.
     */
    public byte[] loadPreferExisting(String preferredKey, String fallbackKey, String videoJsonIfMissing) {
        if (objectStorage.exists(preferredKey)) {
            return read(preferredKey);
        }
        if (fallbackKey != null && !fallbackKey.isBlank() && objectStorage.exists(fallbackKey)) {
            return read(fallbackKey);
        }
        return loadOrRender(preferredKey, videoJsonIfMissing);
    }

    public String readMp4ObjectKey(String videoJson) {
        if (videoJson == null || videoJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(videoJson).path("mp4ObjectKey").asText(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] read(String storageKey) {
        try (var in = objectStorage.get(storageKey)) {
            return in.readAllBytes();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read mp4: " + storageKey, ex);
        }
    }

    public static String keyFor(String assetId, String language, String pace) {
        return "videos/" + assetId + "/" + language + "-" + pace + ".mp4";
    }
}

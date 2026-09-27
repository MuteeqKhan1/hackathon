package com.learningplatform.content.video;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StoryboardMp4RendererTest {

    @Test
    void renderMp4_producesNonEmptyMp4Bytes() {
        StoryboardMp4Renderer renderer = new StoryboardMp4Renderer(new ObjectMapper());
        String json = """
                {"title":"Demo Lesson","scenes":[
                  {"sequence":1,"narration":"Hello students","onScreenText":"Work and Energy","durationSeconds":2,
                   "citedSectionIds":["11111111-1111-1111-1111-111111111111"]}
                ],"citedSectionIds":["11111111-1111-1111-1111-111111111111"]}
                """;
        byte[] mp4 = renderer.renderMp4(json);
        assertThat(mp4.length).isGreaterThan(1000);
        // ISO BMFF / MP4 files typically start with size + 'ftyp'
        String header = new String(mp4, 4, 4);
        assertThat(header).isEqualTo("ftyp");
    }
}

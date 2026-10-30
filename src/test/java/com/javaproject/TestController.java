package com.javaproject;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.util.LinkedMultiValueMap;

import com.javaproject.beans.BoardGame;
import com.javaproject.beans.Review;
import com.javaproject.database.DatabaseAccess;

@SpringBootTest
@AutoConfigureMockMvc
class TestController {

    private DatabaseAccess da;
    private MockMvc mockMvc;

    @Autowired
    public void setDatabase(DatabaseAccess da) {
        this.da = da;
    }

    @Autowired
    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void testAnonymousCannotOpenReviewEditPage() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long gameId = boardGames.get(0).getId();
        List<Review> reviews = da.getReviews(gameId);
        Long reviewId = reviews.get(0).getId();

        mockMvc.perform(get("/{gameId}/reviews/{id}", gameId, reviewId))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testRegularUserCannotOpenReviewEditPage() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long gameId = boardGames.get(0).getId();
        List<Review> reviews = da.getReviews(gameId);
        Long reviewId = reviews.get(0).getId();

        mockMvc.perform(get("/{gameId}/reviews/{id}", gameId, reviewId)
                .with(user("bugs").roles("USER")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/permission-denied"));
    }

    @Test
    void testManagerCanOpenReviewEditPage() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long gameId = boardGames.get(0).getId();
        List<Review> reviews = da.getReviews(gameId);
        Long reviewId = reviews.get(0).getId();

        mockMvc.perform(get("/{gameId}/reviews/{id}", gameId, reviewId)
                .with(user("daffy").roles("USER", "MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("secured/addReview"));
    }

    @Test
    void testManagerCannotOpenReviewWithMismatchedGameId() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        if (boardGames.size() < 2) {
            throw new AssertionError("Test requires at least two board games.");
        }

        Long originalGameId = boardGames.get(0).getId();
        Long differentGameId = boardGames.get(1).getId();

        List<Review> reviews = da.getReviews(originalGameId);
        if (reviews.isEmpty()) {
            throw new AssertionError("Test requires at least one review.");
        }

        Long reviewId = reviews.get(0).getId();

        mockMvc.perform(get("/{gameId}/reviews/{id}", differentGameId, reviewId)
                .with(user("daffy").roles("USER", "MANAGER")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/permission-denied"));
    }

    @Test
    void testRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void testAddBoardGame() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();

        requestParams.add("name", "onecard");
        requestParams.add("level", "1");
        requestParams.add("minPlayers", "2");
        requestParams.add("maxPlayers", "+");
        requestParams.add("gameType", "Party Game");

        int origSize = da.getBoardGames().size();
        mockMvc.perform(post("/boardgameAdded").params(requestParams)
                .with(user("daffy").roles("USER", "MANAGER"))
                .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andDo(print());
        int newSize = da.getBoardGames().size();
        assertEquals(newSize, origSize + 1);
    }

    @Test
    void testEditReview() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long boardgameId = boardGames.get(0).getId();

        List<Review> reviews = da.getReviews(boardgameId);
        Review review = reviews.get(0);
        Long reviewId = review.getId();

        review.setText("Edited text");

        mockMvc.perform(post("/reviewAdded").flashAttr("review", review)
                .with(user("daffy").roles("USER", "MANAGER"))
                .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/" + review.getGameId() + "/reviews"));

        review = da.getReview(reviewId);
        assertEquals("Edited text", review.getText());
    }

    @Test
    void testRegularUserCannotEditReview() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long boardgameId = boardGames.get(0).getId();

        List<Review> reviews = da.getReviews(boardgameId);
        Review review = reviews.get(0);
        Long reviewId = review.getId();
        String originalText = review.getText();

        review.setText("Unauthorized edit attempt");

        mockMvc.perform(post("/reviewAdded")
                .flashAttr("review", review)
                .with(user("bugs").roles("USER"))
                .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/permission-denied"));

        Review unchangedReview = da.getReview(reviewId);
        assertEquals(originalText, unchangedReview.getText());
    }

    @Test
    void testManagerCannotEditReviewWithMismatchedGameId() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        if (boardGames.size() < 2) {
            throw new AssertionError("Test requires at least two board games.");
        }

        Long originalGameId = boardGames.get(0).getId();
        Long differentGameId = boardGames.get(1).getId();

        List<Review> reviews = da.getReviews(originalGameId);
        if (reviews.isEmpty()) {
            throw new AssertionError("Test requires at least one review.");
        }

        Review review = reviews.get(0);
        Long reviewId = review.getId();
        String originalText = review.getText();

        review.setGameId(differentGameId);
        review.setText("Mismatched game ID edit attempt");

        mockMvc.perform(post("/reviewAdded")
                .flashAttr("review", review)
                .with(user("daffy").roles("USER", "MANAGER"))
                .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/permission-denied"));

        Review unchangedReview = da.getReview(reviewId);
        assertEquals(originalGameId, unchangedReview.getGameId());
        assertEquals(originalText, unchangedReview.getText());
    }

    @Test
    void testRegularUserCannotDeleteReview() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long boardgameId = boardGames.get(0).getId();

        List<Review> reviews = da.getReviews(boardgameId);
        Long reviewId = reviews.get(0).getId();
        int originalSize = reviews.size();

        mockMvc.perform(post("/deleteReview/{id}", reviewId)
                .with(user("bugs").roles("USER"))
                .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/permission-denied"));

        assertNotNull(da.getReview(reviewId));
        assertEquals(originalSize, da.getReviews(boardgameId).size());
    }

    @Test
    void testDeleteReview() throws Exception {
        List<BoardGame> boardGames = da.getBoardGames();
        Long boardgameId = boardGames.get(0).getId();

        List<Review> reviews = da.getReviews(boardgameId);
        Long reviewId = reviews.get(0).getId();

        int origSize = reviews.size();

        mockMvc.perform(post("/deleteReview/{id}", reviewId)
                .with(user("daffy").roles("USER", "MANAGER"))
                .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/" + boardgameId + "/reviews"));

        int newSize = da.getReviews(boardgameId).size();

        assertEquals(newSize, origSize - 1);
    }
    @Test
    void testManagerCanCreateBoardGameThroughApi() throws Exception {
        String json = """
                {
                    "name": "api-security-test-game",
                    "level": 1,
                    "minPlayers": 2,
                    "maxPlayers": "4",
                    "gameType": "Party Game"
                }
                """;

        mockMvc.perform(post("/boardgames")
                .contentType("application/json")
                .content(json)
                .with(user("daffy").roles("USER", "MANAGER"))
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    void testRegularUserCannotCreateBoardGameThroughApi() throws Exception {
        String json = """
                {
                    "name": "api-user-denied-test-game",
                    "level": 1,
                    "minPlayers": 2,
                    "maxPlayers": "4",
                    "gameType": "Party Game"
                }
                """;

        int originalSize = da.getBoardGames().size();

        mockMvc.perform(post("/boardgames")
                .contentType("application/json")
                .content(json)
                .with(user("bugs").roles("USER"))
                .with(csrf()))
                .andExpect(status().isFound())
		.andExpect(redirectedUrl("/permission-denied"));

        assertEquals(originalSize, da.getBoardGames().size());
    }

    @Test
    void testAnonymousCannotCreateBoardGameThroughApi() throws Exception {
        String json = """
                {
                    "name": "api-anonymous-denied-test-game",
                    "level": 1,
                    "minPlayers": 2,
                    "maxPlayers": "4",
                    "gameType": "Party Game"
                }
                """;

        mockMvc.perform(post("/boardgames")
                .contentType("application/json")
                .content(json)
                .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testBoardGamesApiGetIsPublic() throws Exception {
        mockMvc.perform(get("/boardgames"))
                .andExpect(status().isOk());
    }
@Test
void testBoardGameApiRejectsMissingCsrfToken() throws Exception {
    String json = """
            {
                "name": "csrf-missing-token-test-game",
                "level": 1,
                "minPlayers": 2,
                "maxPlayers": "4",
                "gameType": "Party Game"
            }
            """;

    int originalSize = da.getBoardGames().size();

    mockMvc.perform(post("/boardgames")
            .contentType("application/json")
            .content(json)
            .with(user("daffy").roles("USER", "MANAGER")))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/permission-denied"));

    assertEquals(originalSize, da.getBoardGames().size());
}
}

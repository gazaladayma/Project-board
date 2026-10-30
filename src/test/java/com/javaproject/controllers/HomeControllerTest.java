package com.javaproject.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.javaproject.beans.BoardGame;
import com.javaproject.beans.Review;
import com.javaproject.database.DatabaseAccess;
import org.springframework.security.core.userdetails.User;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private DatabaseAccess da;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JdbcUserDetailsManager jdbcUserDetailsManager;

    @InjectMocks
    private HomeController controller;

    @Test
    void testNewUserPage() {
        Model model = new ExtendedModelMap();

        assertEquals("new-user", controller.newUser(model));
        assertEquals(
                java.util.List.of("ROLE_MANAGER", "ROLE_USER"),
                model.getAttribute("authorities"));
    }

    @Test
    void testAddUserWithBlankUsername() {
        Model model = new ExtendedModelMap();

        String result = controller.addUser(
                "   ", "password", java.util.List.of("ROLE_USER"),
                model, new RedirectAttributesModelMap());

        assertEquals("new-user", result);
        assertNotNull(model.getAttribute("errorMsg"));
    }

    @Test
    void testAddUserWithBlankPassword() {
        Model model = new ExtendedModelMap();

        String result = controller.addUser(
                "newuser", "   ", java.util.List.of("ROLE_USER"),
                model, new RedirectAttributesModelMap());

        assertEquals("new-user", result);
        assertNotNull(model.getAttribute("errorMsg"));
    }

    @Test
    void testAddUserWithoutSelectedRole() {
        Model model = new ExtendedModelMap();

        String result = controller.addUser(
                "newuser", "password", null,
                model, new RedirectAttributesModelMap());

        assertEquals("new-user", result);
        assertNotNull(model.getAttribute("errorMsg"));
    }

    @Test
    void testAddUserWithInvalidRole() {
        Model model = new ExtendedModelMap();

        String result = controller.addUser(
                "newuser", "password", java.util.List.of("ROLE_ADMIN"),
                model, new RedirectAttributesModelMap());

        assertEquals("new-user", result);
        assertNotNull(model.getAttribute("errorMsg"));
    }

    @Test
    void testAddUserWhenUsernameAlreadyExists() {
        when(jdbcUserDetailsManager.userExists("existinguser"))
                .thenReturn(true);

        Model model = new ExtendedModelMap();

        String result = controller.addUser(
                " existinguser ", "password",
                java.util.List.of("ROLE_USER"),
                model, new RedirectAttributesModelMap());

        assertEquals("new-user", result);
        assertNotNull(model.getAttribute("errorMsg"));
    }

    @Test
    void testAddUserSuccessfullyWithUserRole() {
        when(jdbcUserDetailsManager.userExists("newuser"))
                .thenReturn(false);
        when(passwordEncoder.encode("password"))
                .thenReturn("encoded-password");

        RedirectAttributesModelMap redirectAttributes =
                new RedirectAttributesModelMap();

        String result = controller.addUser(
                " newuser ", "password",
                java.util.List.of("ROLE_USER"),
                new ExtendedModelMap(), redirectAttributes);

        assertEquals("redirect:/", result);
        assertEquals(
                "User successfully added!",
                redirectAttributes.getFlashAttributes().get("userAddedMsg"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(jdbcUserDetailsManager).createUser(captor.capture());

        assertEquals("newuser", captor.getValue().getUsername());
        assertEquals("encoded-password", captor.getValue().getPassword());
        assertTrue(captor.getValue().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void testAddUserSuccessfullyWithManagerRole() {
        when(jdbcUserDetailsManager.userExists("manager"))
                .thenReturn(false);
        when(passwordEncoder.encode("password"))
                .thenReturn("encoded-password");

        controller.addUser(
                "manager", "password",
                java.util.List.of("ROLE_MANAGER"),
                new ExtendedModelMap(), new RedirectAttributesModelMap());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(jdbcUserDetailsManager).createUser(captor.capture());

        assertTrue(captor.getValue().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_MANAGER")));
    }

    @Test
    void testAddUserSuccessfullyWithBothRoles() {
        when(jdbcUserDetailsManager.userExists("bothroles"))
                .thenReturn(false);
        when(passwordEncoder.encode("password"))
                .thenReturn("encoded-password");

        controller.addUser(
                "bothroles", "password",
                java.util.List.of("ROLE_MANAGER", "ROLE_USER"),
                new ExtendedModelMap(), new RedirectAttributesModelMap());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(jdbcUserDetailsManager).createUser(captor.capture());

        assertEquals(2, captor.getValue().getAuthorities().size());
    }

    @Test
    void testAddReviewPage() {
        BoardGame boardGame = new BoardGame();
        boardGame.setId(1L);

        when(da.getBoardGame(1L)).thenReturn(boardGame);

        Model model = new ExtendedModelMap();

        String result = controller.addReview(1L, model);

        assertEquals("secured/addReview", result);
        assertEquals(boardGame, model.getAttribute("boardgame"));
        assertNotNull(model.getAttribute("review"));
    }

    @Test
    void testAddBoardGamePage() {
        Model model = new ExtendedModelMap();

        String result = controller.addBoardGame(model);

        assertEquals("secured/addBoardGame", result);
        assertNotNull(model.getAttribute("boardgame"));
    }

    @Test
    void testReviewAddedAsNewReview() {
        Review review = new Review();
        review.setGameId(1L);
        review.setText("Great game!");

        when(da.addReview(review)).thenReturn(1);

        String result = controller.reviewAdded(review, null);

        assertEquals("redirect:/1/reviews", result);
        verify(da).addReview(review);
    }

    @Test
    void testDeleteReviewWhenReviewDoesNotExist() {
        when(da.getReview(99L)).thenReturn(null);

        String result = controller.deleteReview(99L);

        assertEquals("redirect:/", result);
    }

    @Test
    void testDeleteReviewSuccessfully() {
        Review review = new Review();
        review.setId(5L);
        review.setGameId(1L);

        when(da.getReview(5L)).thenReturn(review);

        String result = controller.deleteReview(5L);

        assertEquals("redirect:/1/reviews", result);
        verify(da).deleteReview(5L);
    }
    @Test
    void testGetBoardgameDetail() {
        BoardGame boardGame = new BoardGame();
        boardGame.setId(1L);

        when(da.getBoardGame(1L)).thenReturn(boardGame);

        Model model = new ExtendedModelMap();

        String result = controller.getBoardgameDetail(1L, model);

        assertEquals("boardgame", result);
        assertEquals(boardGame, model.getAttribute("boardgame"));
    }

    @Test
    void testGetReviews() {
        BoardGame boardGame = new BoardGame();
        boardGame.setId(1L);

        Review review = new Review();
        review.setGameId(1L);
        review.setText("Great game!");

        when(da.getBoardGame(1L)).thenReturn(boardGame);
        when(da.getReviews(1L)).thenReturn(java.util.List.of(review));

        Model model = new ExtendedModelMap();

        String result = controller.getReviews(1L, model);

        assertEquals("review", result);
        assertEquals(boardGame, model.getAttribute("boardgame"));
        assertEquals(java.util.List.of(review), model.getAttribute("reviews"));
    }

    @Test
    void testGoToUserSecured() {
        assertEquals("secured/user/index", controller.goToUserSecured());
    }

    @Test
    void testGoToManagerSecured() {
        assertEquals("secured/manager/index", controller.goToManagerSecured());
    }

    @Test
    void testGoToSecured() {
        assertEquals("secured/gateway", controller.goToSecured());
    }

    @Test
    void testGoToLogin() {
        assertEquals("login", controller.goToLogin());
    }

    @Test
    void testGoToDenied() {
        assertEquals("error/permission-denied", controller.goToDenied());
    }
}

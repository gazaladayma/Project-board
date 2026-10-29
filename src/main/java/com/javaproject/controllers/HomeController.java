package com.javaproject.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.javaproject.beans.BoardGame;
import com.javaproject.beans.Review;
import com.javaproject.database.DatabaseAccess;

@Controller
public class HomeController {

    @Autowired
    DatabaseAccess da;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcUserDetailsManager jdbcUserDetailsManager;

    @GetMapping("/newUser")
    public String newUser() {
        return "new-user";
    }

    @PostMapping("/addUser")
    public String addUser(@RequestParam String userName,
            @RequestParam String password,
            Model model,
            RedirectAttributes redirectAttrs) {

        String normalizedUserName = userName.trim();

        if (normalizedUserName.isEmpty() || password.isBlank()) {
            model.addAttribute("errorMsg", "User name and password are required.");
            return "new-user";
        }

        if (jdbcUserDetailsManager.userExists(normalizedUserName)) {
            model.addAttribute("errorMsg",
                    "User name already exists. Try a different user name.");
            return "new-user";
        }

        User user = new User(
                normalizedUserName,
                passwordEncoder.encode(password),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        jdbcUserDetailsManager.createUser(user);
        redirectAttrs.addFlashAttribute(
                "userAddedMsg", "User successfully added!");

        return "redirect:/";
    }

    @GetMapping("/")
    public String goHome(Model model) {
        List<BoardGame> boardgames = da.getBoardGames();
        model.addAttribute("boardgames", boardgames);
        return "index";
    }

    @GetMapping("/{id}")
    public String getBoardgameDetail(@PathVariable Long id, Model model) {
        model.addAttribute("boardgame", da.getBoardGame(id));
        return "boardgame";
    }

    @GetMapping("/{id}/reviews")
    public String getReviews(@PathVariable Long id, Model model) {
        model.addAttribute("boardgame", da.getBoardGame(id));
        model.addAttribute("reviews", da.getReviews(id));
        return "review";
    }

    @GetMapping("/secured/addReview/{id}")
    public String addReview(@PathVariable Long id, Model model) {
        model.addAttribute("boardgame", da.getBoardGame(id));
        model.addAttribute("review", new Review());

        return "secured/addReview";
    }

    // Edit pages are manager-only; also validate the review/game association.
    @GetMapping("/{gameId}/reviews/{id}")
    public String editReview(@PathVariable Long gameId, @PathVariable Long id, Model model) {
        Review review = da.getReview(id);
        BoardGame boardgame = da.getBoardGame(gameId);

        if (review == null || boardgame == null
                || !java.util.Objects.equals(review.getGameId(), gameId)) {
            return "redirect:/permission-denied";
        }

        model.addAttribute("review", review);
        model.addAttribute("boardgame", boardgame);
        return "secured/addReview";
    }

    @GetMapping("/secured/addBoardGame")
    public String addBoardGame(Model model) {
        model.addAttribute("boardgame", new BoardGame());
        return "secured/addBoardGame";
    }

    @PostMapping("/boardgameAdded")
    public String boardgameAdded(@ModelAttribute BoardGame boardgame) {
        Long returnValue = da.addBoardGame(boardgame);
        System.out.println("return value is: " + returnValue);
        return "redirect:/";
    }

    @PostMapping("/reviewAdded")
    public String reviewAdded(
            @ModelAttribute Review review,
            Authentication authentication) {

        int returnValue;

        // Editing an existing review is restricted to managers.
        if (review.getId() != null) {
            boolean isManager = authentication != null
                    && authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_MANAGER"));

            if (!isManager) {
                return "redirect:/permission-denied";
            }

            // Confirm the review exists and belongs to the submitted game.
            Review existingReview = da.getReview(review.getId());
            if (existingReview == null
                    || !java.util.Objects.equals(
                            existingReview.getGameId(), review.getGameId())) {
                return "redirect:/permission-denied";
            }

            returnValue = da.editReview(review);
        } else {
            // Authenticated users may submit new reviews.
            returnValue = da.addReview(review);
        }

        System.out.println("return value is: " + returnValue);
        return "redirect:/" + review.getGameId() + "/reviews";
    }

    @PostMapping("/deleteReview/{id}")
    public String deleteReview(@PathVariable Long id) {
        Review review = da.getReview(id);

        if (review == null) {
            return "redirect:/";
        }

        Long gameId = review.getGameId();
        da.deleteReview(id);
        return "redirect:/" + gameId + "/reviews";
    }

    @GetMapping("/user")
    public String goToUserSecured() {
        return "secured/user/index";
    }

    @GetMapping("/manager")
    public String goToManagerSecured() {
        return "secured/manager/index";
    }

    @GetMapping("/secured")
    public String goToSecured() {
        return "secured/gateway";
    }

    @GetMapping("/login")
    public String goToLogin() {
        return "login";
    }

    @GetMapping("/permission-denied")
    public String goToDenied() {
        return "error/permission-denied";
    }
}

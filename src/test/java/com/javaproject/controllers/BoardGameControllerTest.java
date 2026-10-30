package com.javaproject.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.javaproject.beans.BoardGame;
import com.javaproject.database.DatabaseAccess;

class BoardGameControllerTest {

    @Test
    void testGetBoardGameWhenRecordExists() {
        BoardGame boardGame = new BoardGame();
        boardGame.setId(1L);
        boardGame.setName("Splendor");

        DatabaseAccess fakeDatabase = new DatabaseAccess() {
            @Override
            public BoardGame getBoardGame(Long id) {
                return boardGame;
            }
        };

        BoardGameController controller = new BoardGameController(fakeDatabase);

        ResponseEntity<Object> response = controller.getBoardGame(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(boardGame, response.getBody());
    }

    @Test
    void testGetBoardGameWhenRecordDoesNotExist() {
        DatabaseAccess fakeDatabase = new DatabaseAccess() {
            @Override
            public BoardGame getBoardGame(Long id) {
                return null;
            }
        };

        BoardGameController controller = new BoardGameController(fakeDatabase);

        ResponseEntity<Object> response = controller.getBoardGame(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testPostBoardGameWhenDatabaseThrowsException() {
        DatabaseAccess fakeDatabase = new DatabaseAccess() {
            @Override
            public Long addBoardGame(BoardGame boardGame) {
                throw new RuntimeException("Simulated duplicate record");
            }
        };

        BoardGameController controller = new BoardGameController(fakeDatabase);

        BoardGame boardGame = new BoardGame();
        boardGame.setName("Splendor");

        ResponseEntity<Object> response = controller.postBoardGame(boardGame);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}

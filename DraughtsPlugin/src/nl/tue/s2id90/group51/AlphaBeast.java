package nl.tue.s2id90.group51;

import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import org10x10.dam.game.Move;

import java.util.Collections;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

/**
 * Implementation of the DraughtsPlayer interface.
 *
 * @author Nimo Beeren & Maas van Apeldoorn
 */
// ToDo: rename this class (and hence this file) to have a distinct name
//       for your player during the tournament
public class AlphaBeast extends DraughtsPlayer {

    private int bestValue = 0;
    private int maxSearchDepth;

    /**
     * Boolean that indicates that the GUI asked the player to stop thinking.
     */
    private boolean stopped;

    AlphaBeast(int maxSearchDepth) {
        super("best.png"); // ToDo: replace with your own icon
        this.maxSearchDepth = maxSearchDepth;
    }

    @Override
    public Move getMove(DraughtsState s) {
        Move bestMove = null;
        bestValue = 0;
        DraughtsNode node = new DraughtsNode(s);    // the root of the search tree
        try {
            for (int depth = 1; depth <= maxSearchDepth; depth++) {
                // compute bestMove and bestValue in a call to alphaBeta
                bestValue = alphaBeta(node, MIN_VALUE, MAX_VALUE, depth);

                // store the bestMove found up until now
                // NB this is not done in case of an AIStoppedException in alphaBeta()
                bestMove = node.getBestMove();

                // print the results for debugging reasons
                System.err.format(
                        "%s: depth=%2d, best move = %5s, value=%d\n",
                        this.getClass().getSimpleName(), depth, bestMove.getChessNotation(), bestValue
                );
            }
        } catch (AIStoppedException ex) {
            /* nothing to do */ }

        if (bestMove == null) {
            System.err.println("no valid move found!");
            return getRandomValidMove(s);
        } else {
            return bestMove;
        }
    }

    /**
     * This method's return value is displayed in the AICompetition GUI.
     *
     * @return the value for the draughts state s as it is computed in a call to
     * getMove(s).
     */
    @Override
    public Integer getValue() {
        return bestValue;
    }

    /**
     * Tries to make alpha-beta search stop. Search should be implemented such
     * that it throws an AIStoppedException when boolean stopped is set to true;
     *
     */
    @Override
    public void stop() {
        stopped = true;
    }

    /**
     * Returns random valid move in state s, or null if no moves exist.
     */
    private Move getRandomValidMove(DraughtsState s) {
        List<Move> moves = s.getMoves();
        Collections.shuffle(moves);
        return moves.isEmpty() ? null : moves.get(0);
    }

    /**
     * Implementation of alpha-beta that automatically chooses the white player
     * as maximizing player and the black player as minimizing player.
     *
     * @param node contains DraughtsState and has field to which the best move
     * can be assigned.
     * @param alpha
     * @param beta
     * @param depth maximum recursion Depth
     * @return the computed value of this node
     * @throws AIStoppedException
     **/
    private int alphaBeta(DraughtsNode node, int alpha, int beta, int depth)
            throws AIStoppedException {
        if (node.getState().isWhiteToMove()) {
            return alphaBetaMax(node, alpha, beta, depth);
        } else {
            return alphaBetaMin(node, alpha, beta, depth);
        }
    }

    /**
     * Does an alpha-beta computation with the given alpha and beta where the
     * player that is to move in node is the minimizing player.
     *
     * @param node  contains DraughtsState and has field to which the best move can be assigned
     * @param alpha the best value for the maximizing player
     * @param beta  the best value for the minimizing player
     * @param depth maximum recursion depth
     * @return the computed value of this node
     * @throws AIStoppedException thrown whenever the boolean stopped has been set to true.
     */
    private int alphaBetaMin(DraughtsNode node, int alpha, int beta, int depth)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        DraughtsState state = node.getState();
        if (depth <= 0) {
            return evaluate(state);
        }
        List<Move> moves = state.getMoves();
        while (!moves.isEmpty()) {
            Move move = moves.get(0);
            state.doMove(move);
            DraughtsNode childNode = new DraughtsNode(state);
            int childValue = alphaBetaMax(childNode, alpha, beta, depth - 1);
            if (childValue < beta) {
                beta = childValue;
                node.setBestMove(move);
            }

            moves.remove(0);
            state.undoMove(move);

            if (beta <= alpha) {
                return alpha;
            }
        }
        return beta;
    }

    /**
     * Does an alpha-beta computation with the given alpha and beta where the
     * player that is to move in node is the maximizing player.
     *
     * @param node  contains DraughtsState and has field to which the best move can be assigned
     * @param alpha the best value for the maximizing player
     * @param beta  the best value for the minimizing player
     * @param depth maximum recursion depth
     * @return the computed value of this node
     * @throws AIStoppedException thrown whenever the boolean stopped has been set to true.
     */
    private int alphaBetaMax(DraughtsNode node, int alpha, int beta, int depth)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        DraughtsState state = node.getState();
        if (depth <= 0) {
            return evaluate(state);
        }
        List<Move> moves = state.getMoves();
        while (!moves.isEmpty()) {
            Move move = moves.get(0);
            state.doMove(move);
            DraughtsNode childNode = new DraughtsNode(state);
            int childValue = alphaBetaMin(childNode, alpha, beta, depth - 1);
            if (childValue > alpha) {
                alpha = childValue;
                node.setBestMove(move);
            }

            moves.remove(0);
            state.undoMove(move);

            if (alpha >= beta) {
                return beta;
            }
        }
        return alpha;
    }

    /**
     * A method that evaluates the given state.
     */
    private int evaluate(DraughtsState state) {
        int numWhite = 0, numBlack = 0;
        int[] pieces = state.getPieces();
        /**
         * A function that counts the number of pieces per player and gives
         * appropriate points
         */
        for (int p : pieces) {
            switch (p) {
            case DraughtsState.WHITEPIECE:
                numWhite = numWhite + 12;
                break;
            case DraughtsState.WHITEKING:
                numWhite = numWhite + 36;
                break;
            case DraughtsState.BLACKPIECE:
                numBlack = numBlack + 12;
                break;
            case DraughtsState.BLACKKING:
                numBlack = numBlack + 36;
                break;
            }
        }
        /**
         * This function calls the auxilary methods for positional play
         */
        for (int i = 1; i <= 50; i++) {
            if (pieces[i] == DraughtsState.WHITEPIECE) {
                numWhite += formationSeekerW(pieces, i, 1);
            } else if (pieces[i] == DraughtsState.BLACKPIECE) {
                numBlack += formationSeekerB(pieces, i, 1);
            }
        }
        /**
         * counts the total number of pieces on the board
         */
        int numberOfPieces = numberOfPieces(pieces);
        /**
         * counts the pieces on the baseline for the first half of the game and
         * gives extra points for them
         */
        if (numberOfPieces >= 25) {
            int baselineWhite = baselineWhite(numWhite, numBlack, pieces, numberOfPieces);
            int baselineBlack = baselineBlack(numWhite, numBlack, pieces, numberOfPieces);
            numWhite = +baselineWhite;
            numWhite = +baselineBlack;
        }
        /**
         * Returns the difference in total scores between black and white
         */
        return numWhite - numBlack;
    }

    /**
     *
     * @param pieces
     * @return number of pieces on the board (regular pieces and kings)
     * @pre pieces is not empty
     */
    public int numberOfPieces(int[] pieces) {
        int piecesOnBoard = 0;
        for (int p : pieces) {
            switch (p) {
            case DraughtsState.WHITEPIECE:
                piecesOnBoard++;
                break;
            case DraughtsState.WHITEKING:
                piecesOnBoard++;
                break;
            case DraughtsState.BLACKPIECE:
                piecesOnBoard++;
                break;
            case DraughtsState.BLACKKING:
                piecesOnBoard++;
                break;
            }
        }
        return piecesOnBoard;
    }

    /**
     * @param numWhite
     * @param numBlack
     * @param pieces
     * @param numberOfPieces
     * @return the number of white pieces on the baseline for the first half of
     * the game to prevent holes in the defence
     * @pre numWhite, numBlack are not 0 && pieces != 0 && numberOfPieces >= 25
     */
    public int baselineWhite(int numWhite, int numBlack, int[] pieces, int numberOfPieces) {
        int baseLinePoints = 0;
        for (int i = 46; i <= 50; i++) {
            if (pieces[i] == DraughtsState.WHITEPIECE || pieces[i] == DraughtsState.WHITEKING) {
                baseLinePoints++;
            }
        }
        return baseLinePoints;
    }

    /**
     * @param numWhite
     * @param numBlack
     * @param pieces
     * @param numberOfPieces
     * @return the number of black pieces on the baseline for the first half of
     * the game to prevent holes in the defence
     * @pre numWhite, numBlack are not 0 && pieces != 0 && numberOfPieces >= 25
     */
    public int baselineBlack(int numWhite, int numBlack, int[] pieces, int numberOfPieces) {
        int baseLinePoints = 0;
        for (int i = 1; i <= 5; i++) {
            if (pieces[i] == DraughtsState.BLACKPIECE || pieces[i] == DraughtsState.BLACKKING) {
                baseLinePoints++;
            }
        }
        return baseLinePoints;
    }
    /**
     * @param pieces
     * @param i
     * @param score
     * @return the score given for the certain formation of max 4 pieces
     * @pre pieces != null, {@code 1 <= i <= 50}
     * @inv score is the given score to a certain formation 
     */
    public int formationSeekerW(int[] pieces, int i, int score) {
        if (score < 4) {
            try {
                if ((i - 1) % 10 < 5) {
                    if (pieces[i + 5] == DraughtsState.WHITEPIECE) {
                        score++;
                        formationSeekerW(pieces, i + 5, score);
                        return score;
                    }

                    if (pieces[i + 6] == DraughtsState.WHITEPIECE) {
                        score++;
                        formationSeekerW(pieces, i + 6, score);
                        return score;
                    }
                } else {
                    if (pieces[i + 4] == DraughtsState.WHITEPIECE) {
                        score++;
                        formationSeekerW(pieces, i + 4, score);
                        return score;
                    }
                    if (pieces[i + 5] == DraughtsState.WHITEPIECE) {
                        score++;
                        formationSeekerW(pieces, i + 5, score);
                        return score;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                return score;
            }
        }
        return score * 2;
    }

    public int formationSeekerB(int[] pieces, int i, int score) {
        if (score < 3) {
            try {
                if ((i - 1) % 10 < 5) {
                    if (pieces[i + 5] == DraughtsState.BLACKPIECE) {
                        score++;
                        formationSeekerB(pieces, i + 5, score);
                        return score;
                    }

                    if (pieces[i + 6] == DraughtsState.BLACKPIECE) {
                        score++;
                        formationSeekerB(pieces, i + 6, score);
                        return score;
                    }
                } else {
                    if (pieces[i + 4] == DraughtsState.BLACKPIECE) {
                        score++;
                        formationSeekerB(pieces, i + 4, score);
                        return score;
                    }
                    if (pieces[i + 5] == DraughtsState.BLACKPIECE) {
                        score++;
                        formationSeekerB(pieces, i + 5, score);
                        return score;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                return score;
            }
        }
        return score * 2;
    }
}

package nl.tue.s2id90.group92;

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
public class AlphaBeast extends DraughtsPlayer {
    private int bestValue = 0;
    private int maxSearchDepth;

    /**
     * Boolean that indicates that the GUI asked the player to stop thinking.
     */
    private boolean stopped;

    AlphaBeast(int maxSearchDepth) {
        super("philosoraptor.jpg");
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
                        "%s: depth = %2d, best move = %5s, value = %d\n",
                        this.getClass().getSimpleName(), depth, bestMove, bestValue
                );
            }
        } catch (AIStoppedException ex) {  /* nothing to do */ }

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
     * Tries to make alpha-beta search stop. Search should be implemented such that it
     * throws an AIStoppedException when boolean stopped is set to true;
     **/
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
     * @param node  contains DraughtsState and has field to which the best move
     *              can be assigned.
     * @param alpha the best value for the maximizing player
     * @param beta  the best value for the minimizing player
     * @param depth maximum recursion depth
     * @return the computed value of this node
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
        int[] pieces = state.getPieces();

        // If a player can make no moves, they lose
        if (state.getMoves().isEmpty()) {
            if (state.isWhiteToMove()) {
                return MIN_VALUE;
            } else {
                return MAX_VALUE;
            }
        }

        return 30 * evalCount(pieces) +
                4 * evalFormations(pieces) +
                2 * evalBaseline(pieces) +
                evalTempi(pieces) +
                evalCenter(pieces);
    }

    /**
     * Counts the number of pieces for each player.
     *
     * @param pieces board state
     * @return difference of piece count between players
     */
    private int evalCount(int[] pieces) {
        int piecesWhite = 0, piecesBlack = 0;
        int kingsWhite = 0, kingsBlack = 0;

        for (int p : pieces) {
            switch (p) {
                case DraughtsState.WHITEPIECE:
                    piecesWhite++;
                    break;
                case DraughtsState.WHITEKING:
                    kingsWhite++;
                    break;
                case DraughtsState.BLACKPIECE:
                    piecesBlack++;
                    break;
                case DraughtsState.BLACKKING:
                    kingsBlack++;
                    break;
            }
        }

        int pieceWeight, kingWeight;
        if (piecesWhite + piecesBlack > 15) {
            pieceWeight = 1;
            kingWeight = 3;
        } else {
            // Kings are more important in end-game
            pieceWeight = 1;
            kingWeight = 5;
        }

        return pieceWeight * (piecesWhite - piecesBlack) +
                kingWeight * (kingsWhite - kingsBlack);
    }

    /**
     * Gets a score based on the strength of formations on the board, i.e. rows of 2 or 3 same-colored pieces.
     *
     * @param pieces board state
     * @return difference of formation strength between players
     */
    private int evalFormations(int[] pieces) {
        int formationWhite = 0, formationBlack = 0;

        for (int i = 1; i <= 50; i++) {
            if (!isEmpty(pieces[i])) {
                boolean isWhite = isWhite(pieces[i]);
                int behindLeft1 = pieceBehind(i, isWhite, true, pieces);
                if (behindLeft1 != -1) {
                    int behindLeft2 = pieceBehind(behindLeft1, isWhite, true, pieces);
                    if (behindLeft2 != -1) {
                        if (isWhite) {
                            formationWhite += 3;
                        } else {
                            formationBlack += 3;
                        }
                    } else {
                        if (isWhite) {
                            formationWhite += 1;
                        } else {
                            formationBlack += 1;
                        }
                    }
                }

                int behindRight1 = pieceBehind(i, isWhite, false, pieces);
                if (behindRight1 != -1) {
                    int behindRight2 = pieceBehind(behindRight1, isWhite, false, pieces);
                    if (behindRight2 != -1) {
                        if (isWhite) {
                            formationWhite += 3;
                        } else {
                            formationBlack += 3;
                        }
                    } else {
                        if (isWhite) {
                            formationWhite += 1;
                        } else {
                            formationBlack += 1;
                        }
                    }
                }
            }
        }

        return formationWhite - formationBlack;
    }

    /**
     * Gets a score based on how many pieces are still on the baseline (more is better).
     * Only affects early to mid game (at least 25 pieces on the board).
     *
     * @param pieces board state
     * @return difference of baseline score between players
     */
    private int evalBaseline(int[] pieces) {
        int baselineWhite = 0, baselineBlack = 0;
        int otherWhite = 0, otherBlack = 0;

        for (int i = 1; i < 6; i++) {
            if (isBlack(pieces[i])) {
                baselineBlack++;
            }
        }
        for (int i = 6; i < 46; i++) {
            if (isWhite(pieces[i])) {
                otherWhite++;
            } else if (isBlack(pieces[i])) {
                otherBlack++;
            }
        }
        for (int i = 46; i <= 50; i++) {
            if (isWhite(pieces[i])) {
                baselineWhite++;
            }
        }

        if (baselineWhite + baselineBlack + otherWhite + otherBlack < 25) {
            return 0;
        }
        return baselineWhite - baselineBlack;
    }

    /**
     * Calculates the tempi difference, to measure how far pieces have advanced in the field.
     * Calculated as the sum of each piece multiplied with its row position.
     *
     * @param pieces board state
     * @return tempi difference
     */
    private int evalTempi(int[] pieces) {
        int tempiWhite = 0, tempiBlack = 0;

        for (int i = 1; i < pieces.length; i++) {
            int row;
            switch (pieces[i]) {
                case DraughtsState.WHITEPIECE:
                    row = getRow(i);
                    tempiWhite += 11 - row;
                    break;
                case DraughtsState.BLACKPIECE:
                    row = getRow(i);
                    tempiBlack += row;
                    break;
            }
        }

        return tempiWhite - tempiBlack;
    }

    /**
     * Counts pieces in the center of the board (e.g. not on the edges).
     *
     * @param pieces board state
     * @return difference in center pieces between players
     */
    private int evalCenter(int[] pieces) {
        int whiteCenter = 0, blackCenter = 0;

        for (int p : pieces) {
            if (!isEmpty(p)) {
                int column = getColumn(p);
                if (!(column == 1 || column == 5)) {
                    if (isWhite(p)) {
                        whiteCenter++;
                    } else {
                        blackCenter++;
                    }
                }
            }
        }

        return whiteCenter - blackCenter;
    }

    private int pieceBehind(int i, boolean isWhite, boolean lookLeft, int[] pieces) {
        if (isWhite) {
            return pieceBehindWhite(i, lookLeft, pieces);
        } else {
            return pieceBehindBlack(i, lookLeft, pieces);
        }
    }

    private int pieceBehindWhite(int piece, boolean lookLeft, int[] pieces) {
        int row = getRow(piece);
        int column = getColumn(piece);
        if (row % 2 == 1) {
            // Odd row
            if (lookLeft) {
                // piece <= 45
                int iBehind = piece + 5;
                if (isWhite(pieces[iBehind])) {
                    return iBehind;
                }
            } else {
                if (column == 5) {
                    // Rightmost column has no square right of it
                    return -1;
                }
                // piece <= 44
                int iBehind = piece + 6;
                if (isWhite(pieces[iBehind])) {
                    return iBehind;
                }
            }
        } else {
            // Even row
            if (row == 10) {
                // Bottom row has no square behind it
                return -1;
            }
            if (lookLeft) {
                if (column == 1) {
                    // Leftmost column has no square left of it
                    return -1;
                }
                // piece <= 40
                int iBehind = piece + 4;
                if (isWhite(pieces[iBehind])) {
                    return iBehind;
                }
            } else {
                // piece <= 40
                int iBehind = piece + 5;
                if (isWhite(pieces[iBehind])) {
                    return iBehind;
                }
            }
        }
        return -1;
    }

    private int pieceBehindBlack(int i, boolean lookLeft, int[] pieces) {
        int row = getRow(i);
        int column = getColumn(i);
        if (row % 2 == 1) {
            // Odd row
            if (row == 1) {
                // Top row has no square above it
                return -1;
            }
            if (lookLeft) {
                // piece >= 10
                int iBehind = i - 5;
                if (isBlack(pieces[iBehind])) {
                    return iBehind;
                }
            } else {
                if (column == 5) {
                    // Top row has no square above it, and
                    // rightmost column has no square right of it
                    return -1;
                }
                // piece >= 11
                int iBehind = i - 4;
                if (isBlack(pieces[iBehind])) {
                    return iBehind;
                }
            }
        } else {
            // Even row
            if (lookLeft) {
                if (column == 1) {
                    // Leftmost column has no square left of it
                    return -1;
                }
                // piece >= 7
                int iBehind = i - 6;
                if (isBlack(pieces[iBehind])) {
                    return iBehind;
                }
            } else {
                if (column == 5) {
                    // Rightmost column has no square right of it
                    return -1;
                }
                // piece >= 6
                int iBehind = i - 5;
                if (isBlack(pieces[iBehind])) {
                    return iBehind;
                }
            }
        }
        return -1;
    }

    private boolean isEmpty(int piece) {
        return !(piece == DraughtsState.WHITEPIECE ||
                piece == DraughtsState.WHITEKING ||
                piece == DraughtsState.BLACKPIECE ||
                piece == DraughtsState.BLACKKING);
    }

    private boolean isWhite(int piece) {
        return piece == DraughtsState.WHITEPIECE ||
                piece == DraughtsState.WHITEKING;
    }

    private boolean isBlack(int piece) {
        return piece == DraughtsState.BLACKPIECE ||
                piece == DraughtsState.BLACKKING;
    }

    private int getRow(int i) {
        return 1 + (i - 1) / 5;
    }

    private int getColumn(int i) {
        return 1 + (i - 1) % 5;
    }
}

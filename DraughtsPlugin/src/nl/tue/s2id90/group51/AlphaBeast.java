package nl.tue.s2id90.group51;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

import java.util.Collections;
import java.util.List;

import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import org10x10.dam.game.Move;

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
                        "%s: depth=%2d, best move = %5s, value=%d\n",
                        this.getClass().getSimpleName(), depth, bestMove.getChessNotation(), bestValue
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
     * @param node contains DraughtsState and has field to which the best move
     * can be assigned.
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

        // Count the number of pieces for each side
        int countWhite = 0, countBlack = 0;
        for (int p : pieces) {
            switch (p) {
            case DraughtsState.WHITEPIECE:
                countWhite += 1;
                break;
            case DraughtsState.WHITEKING:
                countWhite += 3;
                break;
            case DraughtsState.BLACKPIECE:
                countBlack += 1;
                break;
            case DraughtsState.BLACKKING:
                countBlack += 3;
                break;
            }
        }
        int countTotal = countWhite + countBlack;
        int countDiff = countWhite - countBlack;

        // Check for formations
        int formationWhite = 0, formationBlack = 0;
        for (int i = 1; i <= 50; i++) {
            if (pieces[i] == DraughtsState.WHITEPIECE) {
                formationWhite += formationSeekerW(pieces, i, 1);
            } else if (pieces[i] == DraughtsState.BLACKPIECE) {
                formationBlack += formationSeekerB(pieces, i, 1);
            }
        }
        int formationDiff = formationWhite - formationBlack;


        // Count number of pieces on the baseline for each side
        int baselineWhite = 0, baselineBlack = 0;
        if (countTotal >= 25) {
            for (int i = 46; i <= 50; i++) {
                if (pieces[i] == DraughtsState.WHITEPIECE || pieces[i] == DraughtsState.WHITEKING) {
                    baselineWhite++;
                }
            }
            for (int i = 1; i <= 5; i++) {
                if (pieces[i] == DraughtsState.BLACKPIECE || pieces[i] == DraughtsState.BLACKKING) {
                    baselineBlack++;
                }
            }
        }
        int baselineDiff = baselineWhite - baselineBlack;

        // Calculate tempi difference
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
        int tempiDiff = tempiWhite - tempiBlack;

        // Return final evaluation with weighted factors
        return 24 * countDiff + 4 * formationDiff + 2 * baselineDiff + tempiDiff;
    }

    private int getRow(int piece) {
        return 1 + (piece - 1) / 5;
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
        return score;
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
        return score;
    }
}

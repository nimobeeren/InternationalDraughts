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
 * @author huub
 */
// ToDo: rename this class (and hence this file) to have a distinct name
//       for your player during the tournament
public class MyDraughtsPlayer extends DraughtsPlayer {
    private int bestValue = 0;
    int maxSearchDepth;

    /**
     * boolean that indicates that the GUI asked the player to stop thinking.
     */
    private boolean stopped;

    public MyDraughtsPlayer(int maxSearchDepth) {
        super("best.png"); // ToDo: replace with your own icon
        this.maxSearchDepth = maxSearchDepth;
    }

    @Override
    public Move getMove(DraughtsState s) {
        Move bestMove = null;
        bestValue = 0;
        DraughtsNode node = new DraughtsNode(s);    // the root of the search tree
        try {
            // compute bestMove and bestValue in a call to alphaBeta
            bestValue = alphaBeta(node, MIN_VALUE, MAX_VALUE, maxSearchDepth);

            // store the bestMove found up until now
            // NB this is not done in case of an AIStoppedException in alphaBeta()
            bestMove = node.getBestMove();

            // print the results for debugging reasons
            System.err.format(
                    "%s: depth= %2d, best move = %5s, value=%d\n",
                    this.getClass().getSimpleName(), maxSearchDepth, bestMove, bestValue
            );
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
     * @return the value for the draughts state s as it is computed in a call to getMove(s).
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
     * returns random valid move in state s, or null if no moves exist.
     */
    Move getRandomValidMove(DraughtsState s) {
        List<Move> moves = s.getMoves();
        Collections.shuffle(moves);
        return moves.isEmpty() ? null : moves.get(0);
    }

    /**
     * Implementation of alpha-beta that automatically chooses the white player
     * as maximizing player and the black player as minimizing player.
     *
     * @param node  contains DraughtsState and has field to which the best move can be assigned.
     * @param alpha
     * @param beta
     * @param depth maximum recursion Depth
     * @return the computed value of this node
     * @throws AIStoppedException
     **/
    int alphaBeta(DraughtsNode node, int alpha, int beta, int depth)
            throws AIStoppedException {
        if (node.getState().isWhiteToMove()) {
            return alphaBetaMax(node, alpha, beta, depth);
        } else {
            return alphaBetaMin(node, alpha, beta, depth);
        }
    }

    /**
     * Does an alpha-beta computation with the given alpha and beta
     * where the player that is to move in node is the minimizing player.
     *
     * @param node  contains DraughtsState and has field to which the best move can be assigned.
     * @param alpha
     * @param beta
     * @param depth maximum recursion Depth
     * @return the compute value of this node
     * @throws AIStoppedException thrown whenever the boolean stopped has been set to true.
     */
    int alphaBetaMin(DraughtsNode node, int alpha, int beta, int depth)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        DraughtsState state = node.getState();
        if (depth <= 0) {
            return evaluate(state);
        }
        List<Move> children = state.getMoves();
        while (!children.isEmpty()) {
            Move move = children.get(0);
            state.doMove(move);
            DraughtsNode childNode = new DraughtsNode(state);
            int childValue = alphaBetaMax(childNode, alpha, beta, depth - 1);
            if (childValue <= beta) {
                beta = childValue;
                node.setBestMove(move);
            }
            if (beta <= alpha) {
                return alpha;
            }
            children.remove(0);
            state.undoMove(move);
        }
        return beta;
    }

    /**
     * Does an alpha-beta computation with the given alpha and beta
     * where the player that is to move in node is the maximizing player.
     *
     * @param node  contains DraughtsState and has field to which the best move can be assigned.
     * @param alpha
     * @param beta
     * @param depth maximum recursion Depth
     * @return the compute value of this node
     * @throws AIStoppedException thrown whenever the boolean stopped has been set to true.
     */
    int alphaBetaMax(DraughtsNode node, int alpha, int beta, int depth)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        DraughtsState state = node.getState();
        if (depth <= 0) {
            return evaluate(state);
        }
        List<Move> children = state.getMoves();
        while (!children.isEmpty()) {
            Move move = children.get(0);
            state.doMove(move);
            DraughtsNode childNode = new DraughtsNode(state);
            int childValue = alphaBetaMin(childNode, alpha, beta, depth - 1);
            if (childValue >= alpha) {
                alpha = childValue;
                node.setBestMove(move);
            }
            if (alpha >= beta) {
                return beta;
            }
            children.remove(0);
            state.undoMove(move);
        }
        return alpha;
    }

    /**
     * A method that evaluates the given state.
     */
    int evaluate(DraughtsState state) {
        int numWhite = 0, numBlack = 0;

        int[] pieces = state.getPieces();
        for (int p : pieces) {
            switch (p) {
                case DraughtsState.WHITEPIECE:
                case DraughtsState.WHITEKING:
                    numWhite++;
                    break;
                case DraughtsState.BLACKPIECE:
                case DraughtsState.BLACKKING:
                    numBlack++;
                    break;
            }
        }

        return numWhite - numBlack;
    }
}

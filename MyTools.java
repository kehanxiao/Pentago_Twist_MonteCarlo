package student_player;
import boardgame.Move;
import boardgame.Board;
import pentago_twist.PentagoBoardState;
import pentago_twist.PentagoCoord;
import pentago_twist.PentagoMove;
import pentago_twist.RandomPentagoPlayer;
import student_player.MyTools;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MyTools {

    private static final int MaxTime = 1650;
// This maxTime works fine on my computer. Regarding of performance,
// I didn't set it too high, so you can adjust it due to your computer performance.
// However, if the code run out of time,
// you can always adjust it to be shorter.
    private static int Mine;

    static class MCNode {
        MCNode parent;
        ArrayList<MCNode> child;
        int player;
        int count;
        double score;
        PentagoBoardState Bstate;
        PentagoMove move;


        MCNode(PentagoBoardState Bstate, PentagoMove move) {
            this.move = move;
            this.Bstate = Bstate;
            this.count = 0;
            this.score = 0;
            this.player = Bstate.getTurnPlayer();
            this.child = new ArrayList<MCNode>();
        }

        // For colon node and branches
        MCNode(MCNode node) {
            this.child = new ArrayList<MCNode>();
            if (node.parent != null)
                this.parent = node.parent;
            this.move = node.move;
            this.Bstate = (PentagoBoardState) node.Bstate.clone();
            this.player = node.player;
            this.count = node.count;
            this.score = node.score;
            ArrayList<MCNode> arr = node.child;
            for (MCNode child : arr) {
                this.child.add(new MCNode(child));
            }
        }
    }

    static int est(PentagoBoardState Bstate, PentagoMove move) {
        if (move == null) {
            return 0;
        }

        PentagoCoord p = move.getMoveCoord();
        int x = p.getX();
        int y = p.getY();
        PentagoBoardState.Piece[][] board = Bstate.getBoard();
        if (((x == 1) && (y == 1)) || ((x == 1) && (y == 4)) || ((x == 4) && (y == 1)) || ((x == 4) && (y == 4))) {
            return 1;
        }
        for (int i = Math.max(x - 1, 0); i <= Math.min(x + 1, 5); i++) {
            for (int j = Math.max(y - 1, 0); j <= Math.min(y + 1, 5); j++) {
                if (!(board[i][j]==PentagoBoardState.Piece.EMPTY)) {
                    return 1;
                }
            }
        }
        return 0;
    }


    static double UCV(int totalVisit, double nodeWin, int nodeVisit) {
        if (nodeVisit == 0) {
            return 0.35+nodeWin;
        }
        return (nodeWin / (double) nodeVisit) + Math.sqrt(2.0 * Math.log(totalVisit) / (double) nodeVisit);
    }

    static double randomPlay(MCNode node) {

        // Colon a node
        MCNode temp = new MCNode(node);
        int i = 0;
        int winner = temp.Bstate.getWinner();
        while ((winner == Board.NOBODY) && (i < 6)) {
            temp.player = temp.player * (-1) + 1;
            // Due to the seed in RandomMove, I will use math.random!!!
            ArrayList<PentagoMove> moves = temp.Bstate.getAllLegalMoves();
            int n = moves.size();
            int r = (int) (Math.random() * n);
            PentagoMove rmove = moves.get(r);
            int k = 0;
            while ((est(temp.Bstate, rmove) == 0) || (k >= 3)) {
                int d = (int) (Math.random() * n);
                rmove = moves.get(d);
            }
            temp.Bstate.processMove(rmove);
            winner = temp.Bstate.getWinner();
            i++;
        }

        if (winner == Mine) {
            return 1;
        } else if (winner == Board.NOBODY) {
            return 0.3;
        }
        temp.parent.score = -1;
        return 0;
    }

    static void backProp(MCNode node, double score) {
        while (node != null) {
            node.count = node.count + 1;
            if ((node.player == Mine) && (node.score != -1))
                node.score = node.score + score;
            node = node.parent;
        }
    }


    public static Move NextMove(PentagoBoardState bstate) {
        Mine = bstate.getTurnPlayer();

        long endTime = System.currentTimeMillis() + MaxTime;

        MCNode root = new MCNode(bstate, null);
        ArrayList<PentagoMove> allMoves = bstate.getAllLegalMoves();
        for (int i = 0; i < allMoves.size(); i++) {
            if (System.currentTimeMillis() > endTime-800) {
                break;
            }
            PentagoMove nMove = allMoves.get(i);
            if (est(bstate, nMove) == 1) {
                PentagoBoardState nBoard = (PentagoBoardState) bstate.clone();
                MCNode nNode = new MCNode(nBoard, nMove);
                nBoard.processMove(nMove);
                nNode.score = Heuristic(nBoard, nMove);
                if (nBoard.getWinner() == Mine) {
                    return nMove;
                } else if (nBoard.getWinner() == Mine * (-1) + 1) {
                    continue;
                }

                ArrayList<PentagoMove> secMoves = nBoard.getAllLegalMoves();
                int k = 0;

                for (int j = 0; j < secMoves.size(); j++) {
                    if (System.currentTimeMillis() > endTime-800) {
                        k=1;
                        break;
                    }
                    PentagoMove secMove = secMoves.get(j);
                    if (est(nBoard, secMove) == 1) {
                        PentagoBoardState secBoard = (PentagoBoardState) nBoard.clone();
                        MCNode secNode = new MCNode(secBoard, secMove);
                        secBoard.processMove(secMove);
                        if (secBoard.getWinner() == Mine * (-1) + 1) {
                            k = 1;
                            continue;
                        } else if (nBoard.getWinner() == Mine) {
                            continue;
                        }
                        secNode.parent = nNode;
                        nNode.child.add(secNode);
                    }
                }
                if (k == 1) {
                    continue;
                }
                nNode.parent = root;
                root.child.add(nNode);
                if (System.currentTimeMillis() > endTime-800) {
                    k=1;
                    break;
                }
            }
        }

        if (root.child.size()==0) {
            return bstate.getRandomMove();
        }


        while (System.currentTimeMillis() < endTime-200) {
            MCNode pnode = root;
            while (pnode.child.size() != 0) {
                int n = pnode.count;
                pnode = Collections.max(pnode.child,
                        Comparator.comparing(x -> UCV(n, x.score, x.count)));
            }
            if ((pnode.Bstate.getWinner() == Mine) && (pnode.move.getPlayerID() == Mine)) {
                backProp(pnode, 1);
                continue;
            } else if (((pnode.Bstate.getWinner() == Mine * (-1) + 1) && (pnode.move.getPlayerID() == Mine * (-1) + 1))) {
                backProp(pnode, -1);
                continue;
            }
            ArrayList<PentagoMove> moves = pnode.Bstate.getAllLegalMoves();
            for (int q = 0; q < moves.size(); q++) {
                if (System.currentTimeMillis() > endTime-300) {
                    break;
                }
                PentagoMove move = moves.get(q);
                if (est(pnode.Bstate, move) == 1) {
                    PentagoBoardState nBoard = (PentagoBoardState) pnode.Bstate.clone();
                    MCNode nNode = new MCNode(nBoard, move);
                    nNode.Bstate.processMove(move);
                    nNode.parent = pnode;
                    pnode.child.add(nNode);
                }
            }

            int n = pnode.child.size();
            if (n==0){
                continue;
            }
            int r = (int) (Math.random() * n);
            MCNode enode = pnode.child.get(r);
            if (System.currentTimeMillis() > endTime-300) {
                break;
            }

            double result = randomPlay(enode);
            backProp(enode, result);

        }

        // Not use uct here, just to remove randomness!!!
        MCNode next = Collections.max(root.child, Comparator.comparing(c -> c.score));
        return next.move;
    }

    static double Heuristic(PentagoBoardState Bstate, PentagoMove move){

        double scrw = 0;
        double scrb = 0;
        double wh;
        double bh;
        double wv;
        double bv;
        double wd1 = 1;
        double bd1 = 1;
        double wd2 = 1;
        double bd2 = 1;
        double wd3 = 1;
        double bd3 = 1;
        double wd4 = 1;
        double bd4 = 1;
        double wd5 = 1;
        double bd5 = 1;
        double wd6 = 1;
        double bd6 = 1;



        PentagoBoardState.Piece[][] board = Bstate.getBoard();

        if (Bstate.getWinner() == Board.NOBODY) {

            for (int i = 0; i < 6; i++) {
                wh = 1;
                bh = 1;
                wv = 1;
                bv = 1;

                for (int j = 0; j < 6; j++) {
                    if (board[i][j]== PentagoBoardState.Piece.WHITE){
                        wh = wh*2;
                        if (!((j==5)&&!(board[i][0]==PentagoBoardState.Piece.WHITE))) {
                            wh = wh*bh/10;
                            bh = Math.max(bh - 10*j, 0);

                        }

                    } else if (board[i][j]==PentagoBoardState.Piece.BLACK){
                        bh = bh*2;
                        if (!((j==5)&&!(board[i][0]==PentagoBoardState.Piece.BLACK))) {
                            bh=bh*wh/10;
                            wh = Math.max(wh - 10*j, 0);

                        }
                    }
                    if (board[j][i]==PentagoBoardState.Piece.WHITE){
                        wv = wv*2;
                        if (!(j==5)&&!(board[0][i]==PentagoBoardState.Piece.WHITE)) {
                            wv = wv*bh/10;
                            bv = Math.max(bv - 10*j, 0);

                        }
                    } else if (board[j][i]==PentagoBoardState.Piece.BLACK){
                        bv = bv*2;
                        if (!((j==5)&&!(board[0][i]==PentagoBoardState.Piece.BLACK))) {
                            bv = bv*wv/10;
                            wv = Math.max(wv - 10*j, 0);

                        }
                    }
                }
                scrw = Math.max(Math.max(wv,wh),scrw);
                scrb = Math.max(Math.max(bv,bh),scrb);

                if (board[i][i]==PentagoBoardState.Piece.WHITE){
                    wd1 = wd1*2;
                    if (!((i==5)&&!(board[0][0]==PentagoBoardState.Piece.WHITE))) {
                        bd1 = Math.max(bd1 - 10*i, 0);
                        wd1 = wd1*bd1/10;
                    }
                } else if (board[i][i]==PentagoBoardState.Piece.BLACK){
                    bd1 = bd1*2;
                    if (!((i==5)&&(!board[0][0].toString().equals(PentagoBoardState.Piece.BLACK.toString())))) {
                        wd1 = Math.max(wd1 - 10*i, 0);
                        bd1 = bd1*wd1/10;
                    }
                }
                if (board[i][5-i]==PentagoBoardState.Piece.WHITE){
                    wd2 = wd2*2;
                    if (!((i==5)&&!(board[0][5]==PentagoBoardState.Piece.WHITE))) {
                        bd2 = Math.max(bd2 - 10*i, 0);
                        wd2 = wd2*bd2/10;
                    }
                } else if (board[i][5-i]==PentagoBoardState.Piece.BLACK){
                    bd2 = bd2*2;
                    if (!((i==5)&&!(board[0][5]==PentagoBoardState.Piece.BLACK))) {
                        wd2 = Math.max(wd2 - 10*i, 0);
                        bd2 = bd2*wd2/10;
                    }
                }
                if ((i-1>=0)&&(board[i-1][i]==PentagoBoardState.Piece.WHITE)){
                    wd3 = wd3*2;
                    bd3 = 0;
                } else if ((i-1>=0)&&!(board[i-1][i]==PentagoBoardState.Piece.BLACK)){
                    bd3 = bd3*2;
                    wd3 = 0;
                }

                if ((i+1<=5)&&(board[i][i+1]==PentagoBoardState.Piece.WHITE)){
                    wd4 = wd4*2;
                    bd4 = 0;
                } else if ((i+1<=5)&&!(board[i][i+1]==PentagoBoardState.Piece.BLACK)){
                    bd4 = bd4*2;
                    wd4= 0;
                }

                if ((4-i>=0)&&(board[4-i][i]==PentagoBoardState.Piece.WHITE)){
                    wd5 = wd5*2;
                    bd5 = 0;
                } else if ((4-i>=0)&&(board[4-i][i]==PentagoBoardState.Piece.BLACK)){
                    bd5 = bd5*2;
                    wd5= 0;
                }

                if ((6-i<=5)&&(board[i][6-i]==PentagoBoardState.Piece.WHITE)){
                    wd6 = wd6*2;
                    bd6 = 0;
                } else if ((6-i<=5)&&(board[i][6-i]==PentagoBoardState.Piece.BLACK)){
                    bd6 = bd6*2;
                    wd6= 0;
                }
            }
            scrw = Math.max(Math.max(Math.max(Math.max(Math.max(Math.max(wd1 , wd2) , wd3) , wd4 ), wd5) , wd6), scrw);
            scrb = Math.max(Math.max(Math.max(Math.max(Math.max(Math.max(bd1 , bd2) , bd3) , bd4 ), bd5) , bd6), scrb);



        }
        else {
            if (Bstate.getWinner() == PentagoBoardState.WHITE) {
                scrw = 2;
                scrb = 0;
            }
            else {
                scrb = 2;
                scrw = 0;
            }
        }

        // Need to invert the score since BLACK would be the MIN Player
        if (Bstate.getTurnPlayer() == PentagoBoardState.WHITE) {
            return scrw/16;
        }

        return scrb/16;

    }

}

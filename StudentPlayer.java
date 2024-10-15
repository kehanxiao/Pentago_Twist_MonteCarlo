
// Student Name: Ke Han Xiao
// Student ID: 260866528
// The maxTime for this program can be adjust in Mytool,
// The program can be terminated with in 2s on my computer,

// but for it to be safe, I set the time of processing to be 1650.
// You can always change the time allowed it in Mytool.
package student_player;

import boardgame.Move;
import pentago_twist.PentagoMove;
import pentago_twist.PentagoPlayer;
import pentago_twist.PentagoBoardState;

/** A player file submitted by a student. */
public class StudentPlayer extends PentagoPlayer {


    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("260866528");
    }

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    public Move chooseMove(PentagoBoardState boardState) {
        // You probably will make separate functions in MyTools.
        // For example, maybe you'll need to load some pre-processed best opening
        // strategies...

        // Is random the best you can do?
        if (boardState.getTurnNumber()<3){
            PentagoMove move1 = new PentagoMove(1,1,0,0, boardState.getTurnPlayer());
            PentagoMove move2 = new PentagoMove(1,4,0,0, boardState.getTurnPlayer());
            PentagoMove move3 = new PentagoMove(4,1,0,0, boardState.getTurnPlayer());
            PentagoMove move4 = new PentagoMove(4,4,0,0, boardState.getTurnPlayer());
            PentagoMove move5 = new PentagoMove(3,3,0,0, boardState.getTurnPlayer());
            PentagoMove move6 = new PentagoMove(2,2,1,1, boardState.getTurnPlayer());
            if (boardState.isLegal(move1)){
                return move1;
            } else if (boardState.isLegal(move2)){
                return move2;
            } else if (boardState.isLegal(move3)){
                return move3;
            } else if (boardState.isLegal(move4)){
                return move4;
            }
            else if (boardState.isLegal(move5)){
                return move5;
            }
            else if (boardState.isLegal(move6)){
                return move6;
            }
        }



        Move myMove = MyTools.NextMove(boardState);


        // Return your move to be processed by the server.
        return myMove;
    }
}
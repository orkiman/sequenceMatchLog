public class Input {
    boolean currentState, rising, falling;

    public void update(boolean newState) {
        if (!currentState && newState) {// rising
            rising = true;
            falling = false;
        } else if (currentState && !newState) {//falling
            rising = false;
            falling = true;
        } else {
            rising = falling = false;
        }
        currentState = newState;
    }
}

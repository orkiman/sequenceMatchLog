public class ThreadSynchronizeTest {


    int counter;
    private synchronized void work(int num,int delay){
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        counter = num ;
        System.out.println(counter);
    }
    void doo(){
         new Thread(() -> {work(1,3000);}).start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {work(2,100);}).start();
    }
}


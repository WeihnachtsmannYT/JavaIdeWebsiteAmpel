public class Main {

    public static void main(String[] args) throws InterruptedException {
        myAmpel.set(0, 255, 0, 0);
        Thread.sleep(1000);
        myAmpel.set(0, 128, 128, 128);
        myAmpel.set(1, 255, 255, 0);
        Thread.sleep(1000);
        myAmpel.set(1, 128, 128, 128);
        myAmpel.set(2, 0, 255, 0);
        Thread.sleep(1000);
        myAmpel.set(2, 128, 128, 128);
    }
}
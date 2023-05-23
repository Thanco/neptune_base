package host.thanco.NeptuneBase;

import java.util.Timer;
import java.util.TimerTask;

public class SaveManager implements Runnable {
    private BaseArrayList baseArrayList;
    private int minsBetweenSave;

    public SaveManager(BaseArrayList baseArrayList) {
        this.baseArrayList = baseArrayList;
    }

    @Override
    public void run() {
        startAutoSave();
    }

    private void startAutoSave() {
        long delay = intMinsTolongMillis(minsBetweenSave);
        Timer autoSaveTimer = new Timer(true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                baseArrayList.saveLists();
            }
        }, 0, delay);

    }

    private long intMinsTolongMillis(int mins) {
        return mins * 60000 ;
    }
}

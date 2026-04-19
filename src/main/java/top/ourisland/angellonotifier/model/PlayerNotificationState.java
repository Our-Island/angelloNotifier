package top.ourisland.angellonotifier.model;

public class PlayerNotificationState {

    int previewCount;
    boolean read;
    long firstSeenAtEpochMs;
    long readAtEpochMs;

    public int previewCount() {
        return previewCount;
    }

    public void previewCount(int previewCount) {
        this.previewCount = previewCount;
    }

    public boolean read() {
        return read;
    }

    public void read(boolean read) {
        this.read = read;
    }

    public long firstSeenAtEpochMs() {
        return firstSeenAtEpochMs;
    }

    public void firstSeenAtEpochMs(long firstSeenAtEpochMs) {
        this.firstSeenAtEpochMs = firstSeenAtEpochMs;
    }

    public long readAtEpochMs() {
        return readAtEpochMs;
    }

    public void readAtEpochMs(long readAtEpochMs) {
        this.readAtEpochMs = readAtEpochMs;
    }

}

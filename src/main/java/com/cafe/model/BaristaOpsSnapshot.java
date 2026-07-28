package com.cafe.model;

/** Read model ngắn hạn cho dashboard vận hành Barista trong một ngày kinh doanh. */
public class BaristaOpsSnapshot {
    private int myMakingCups;
    private int myCompletedCups;
    private int myRemakeCount;
    private int myWasteCount;
    private long myAveragePreparationSeconds;
    private int branchWaitingCups;
    private int branchMakingCups;
    private int branchReadyCups;
    private int branchBlockedCups;
    private int branchRemakeCount;
    private int expiredPrepBatchCount;

    public int getMyMakingCups() { return myMakingCups; }
    public void setMyMakingCups(int myMakingCups) { this.myMakingCups = Math.max(0, myMakingCups); }
    public int getMyCompletedCups() { return myCompletedCups; }
    public void setMyCompletedCups(int myCompletedCups) { this.myCompletedCups = Math.max(0, myCompletedCups); }
    public int getMyRemakeCount() { return myRemakeCount; }
    public void setMyRemakeCount(int myRemakeCount) { this.myRemakeCount = Math.max(0, myRemakeCount); }
    public int getMyWasteCount() { return myWasteCount; }
    public void setMyWasteCount(int myWasteCount) { this.myWasteCount = Math.max(0, myWasteCount); }
    public long getMyAveragePreparationSeconds() { return myAveragePreparationSeconds; }
    public void setMyAveragePreparationSeconds(long seconds) { this.myAveragePreparationSeconds = Math.max(0, seconds); }
    public int getBranchWaitingCups() { return branchWaitingCups; }
    public void setBranchWaitingCups(int value) { this.branchWaitingCups = Math.max(0, value); }
    public int getBranchMakingCups() { return branchMakingCups; }
    public void setBranchMakingCups(int value) { this.branchMakingCups = Math.max(0, value); }
    public int getBranchReadyCups() { return branchReadyCups; }
    public void setBranchReadyCups(int value) { this.branchReadyCups = Math.max(0, value); }
    public int getBranchBlockedCups() { return branchBlockedCups; }
    public void setBranchBlockedCups(int value) { this.branchBlockedCups = Math.max(0, value); }
    public int getBranchRemakeCount() { return branchRemakeCount; }
    public void setBranchRemakeCount(int value) { this.branchRemakeCount = Math.max(0, value); }
    public int getExpiredPrepBatchCount() { return expiredPrepBatchCount; }
    public void setExpiredPrepBatchCount(int value) { this.expiredPrepBatchCount = Math.max(0, value); }

    public boolean isHasMyPreparationTime() { return myAveragePreparationSeconds > 0; }

    /** Hiển thị gọn, không dùng làm SLA/ranking nhân sự. */
    public String getMyAveragePreparationDisplay() {
        if (!isHasMyPreparationTime()) return "Chưa có dữ liệu";
        long minutes = myAveragePreparationSeconds / 60;
        long seconds = myAveragePreparationSeconds % 60;
        return minutes > 0 ? minutes + " phút " + seconds + " giây" : seconds + " giây";
    }
}

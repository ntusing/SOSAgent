package edu.clemson.openflow.sos.agent;

public abstract class SendingStrategy {

     int totalChannels;
     int currentChannel = 1;

    public SendingStrategy(int totalChannels){
        this.totalChannels = totalChannels;
    }

    abstract int channelToSendOn();

    public int getCurrentChannel() {
        return currentChannel;
    }

    public int getTotalChannels() {
        return totalChannels;
    }
}

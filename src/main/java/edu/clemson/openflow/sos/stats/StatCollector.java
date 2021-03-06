package edu.clemson.openflow.sos.stats;

/**
 * @author Khayam Anjam kanjam@g.clemson.edu
 * This class collects stats like active SOS connections, total connected hosts.
 */
public class StatCollector {

    private static int connectedHosts;
    private static int totalOpenConnections;
    private static int packetDropCount;
    private static int usedBufferIndices;

    private static StatCollector statCollector = new StatCollector();

    public static void hostAdded() {
        connectedHosts ++;
    }
    public static void hostRemoved() {
        connectedHosts --;
    }
    public static void connectionAdded() {
        ++ totalOpenConnections;
    }
    public void packetDropped(){
        ++packetDropCount;
    }

    public void bufferIndexUsed() {
        ++usedBufferIndices;
    }
    public void bufferIndexRemoved() {
        --usedBufferIndices;
    }

    public int getPacketDropCount() {
        return packetDropCount;
    }

    public int getUsedBufferIndices() {
        return usedBufferIndices;
    }

    public void setPacketDropCount(int packetDropCount) {
        StatCollector.packetDropCount = packetDropCount;
    }

    public static void connectionRemoved() {
        -- totalOpenConnections ;
    }
    public static StatCollector getStatCollector() {
        return statCollector;
    }

    public int getConnectedHosts() {
        return connectedHosts;
    }

    public int getTotalOpenConnections() {
        return totalOpenConnections;
    }
}

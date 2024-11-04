package com.client.clientUtils;

import java.util.List;

public class ShipPosition {
    int size;
    List<Position> positions;
    
    public ShipPosition(int size, List<Position> positions) {
        this.size = size;
        this.positions = positions;
    }

    public List<Position> getPositions() {
        return positions;
    }
}

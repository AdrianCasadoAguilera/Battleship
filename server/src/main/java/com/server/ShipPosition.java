package com.server;

import java.util.List;

public class ShipPosition {
    int size;
    List<Position> positions;
    
    ShipPosition(int size, List<Position> positions) {
        this.size = size;
        this.positions = positions;
    }

    public List<Position> getPositions() {
        return positions;
    }
}

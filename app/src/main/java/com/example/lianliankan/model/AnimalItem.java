package com.example.lianliankan.model;

public class AnimalItem {
    private int animalId;
    private int imageResId;
    private int row;
    private int col;
    private boolean matched;

    public AnimalItem(int animalId, int imageResId, int row, int col) {
        this.animalId = animalId;
        this.imageResId = imageResId;
        this.row = row;
        this.col = col;
        this.matched = false;
    }

    public int getAnimalId() { return animalId; }
    public void setAnimalId(int animalId) { this.animalId = animalId; }
    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }
    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }

    @Override
    public String toString() {
        return "AnimalItem{row=" + row + ", col=" + col +
                ", id=" + animalId + ", matched=" + matched + "}";
    }
}
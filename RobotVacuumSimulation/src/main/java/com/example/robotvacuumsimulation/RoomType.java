package com.example.robotvacuumsimulation;

public enum RoomType {

    SALON("Salon", "Buyuk acik alan", "S"),
    BEDROOM("Yatak Odasi", "Yatak, Gardrop", "B"),
    OFFICE("Ofis", "Masa siralari", "O");

    public final String displayName;
    public final String description;
    public final String icon;

    RoomType(String displayName, String description, String icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon        = icon;
    }
}

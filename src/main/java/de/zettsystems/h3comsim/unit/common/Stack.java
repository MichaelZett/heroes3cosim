package de.zettsystems.h3comsim.unit.common;

public class Stack<T extends Unit> {
    private T unit;
    private int number;

    public Stack(T unit, int number) {
        this.unit = unit;
        this.number = number;
    }
}

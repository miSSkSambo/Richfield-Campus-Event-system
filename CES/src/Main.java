package src;

import src.ui.ConsoleUI;

/**
 * Campus Event Management System - Main Entry Point
 * Richfield Campus Event Management System
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║       RICHFIELD CAMPUS EVENT MANAGEMENT SYSTEM           ║");
        System.out.println("║                  Version 1.0.0                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        ConsoleUI ui = new ConsoleUI();
        ui.start();
    }
}

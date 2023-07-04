package bisq.desktop_app;

import bisq.desktop.JavaFxExecutable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DesktopApp {
    // A class named Main is required as distribution's entry point.
    // See https://github.com/javafxports/openjdk-jfx/issues/236
    public static void main(String[] args) {
        new JavaFxExecutable(args);
    }
}

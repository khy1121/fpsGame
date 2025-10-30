package com.fpsgame.common;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * 표준 출력/에러를 UTF-8로 고정하여 한글 깨짐을 줄이는 유틸.
 * (Windows 콘솔에서도 UTF-8 코드페이지/터미널에서 정상 표시될 확률을 높임)
 */
public final class Utf8 {
    private Utf8() {}

    /** System.out/System.err을 UTF-8 PrintStream으로 교체 */
    public static void installConsoleUtf8() {
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        } catch (Exception ignore) {}
        try {
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        } catch (Exception ignore) {}
    }
}


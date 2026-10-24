/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2026 huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.task;

import com.sun.net.httpserver.HttpServer;
import org.jackhuang.hmcl.util.CacheRepository;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/// Verifies cache-backed downloads under different HTTP response metadata policies.
@NotNullByDefault
public final class CacheFileTaskTest {

    /// A successful response without an ETag still produces a usable cached file.
    @Test
    public void testDownloadWithoutETag(@TempDir Path tempDirectory) throws Exception {
        byte[] content = "Minecraft client".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        try {
            server.createContext("/client.jar", exchange -> {
                exchange.sendResponseHeaders(200, content.length);
                exchange.getResponseBody().write(content);
                exchange.close();
            });
            server.start();

            CacheRepository repository = new CacheRepository();
            repository.changeDirectory(tempDirectory);
            String url = "http://%s:%d/client.jar".formatted(
                    server.getAddress().getAddress().getHostAddress(), server.getAddress().getPort());
            CacheFileTask task = new CacheFileTask(url);
            task.setCacheRepository(repository);

            assertTrue(task.executor().test());
            Path result = task.getResult();
            assertNotNull(result);
            assertTrue(Files.isRegularFile(result));
            assertArrayEquals(content, Files.readAllBytes(result));
        } finally {
            server.stop(0);
        }
    }
}

package com.cafe.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSourceContractTest {
    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void jsp_files_are_declarative_and_protected() throws IOException {
        for (Path jsp : files(WEBAPP, ".jsp")) {
            String source = Files.readString(jsp);
            String withoutComments = source.replaceAll("(?s)<%--.*?--%>", "");
            assertFalse(Pattern.compile("<%(?!@)").matcher(withoutComments).find(),
                    () -> "JSP chứa scriptlet/Java: " + jsp);
            assertTrue(jsp.startsWith(WEBAPP.resolve("WEB-INF")),
                    () -> "JSP nghiệp vụ phải nằm dưới WEB-INF: " + jsp);
            assertFalse(source.matches("(?s).*<%@\\s*page\\s+import=.*"),
                    () -> "JSP không được import Java: " + jsp);
        }
    }

    @Test
    void every_literal_dispatcher_points_to_an_existing_view() throws IOException {
        Pattern dispatcher = Pattern.compile("getRequestDispatcher\\(\"([^\"]+)\"\\)");
        for (Path javaFile : files(MAIN_JAVA.resolve("com/cafe/controller"), ".java")) {
            Matcher matcher = dispatcher.matcher(Files.readString(javaFile));
            while (matcher.find()) {
                String target = matcher.group(1);
                if (!target.endsWith(".jsp")) continue;
                Path resolved = WEBAPP.resolve(target.substring(1));
                assertTrue(Files.isRegularFile(resolved),
                        () -> "Dispatcher không tồn tại: " + target + " trong " + javaFile);
            }
        }
    }

    @Test
    void servlet_mappings_are_unique() throws IOException {
        Pattern annotation = Pattern.compile("@WebServlet\\(([^)]*)\\)");
        Pattern path = Pattern.compile("\"(/[^\"]*)\"");
        Map<String, Path> owners = new HashMap<>();
        for (Path javaFile : files(MAIN_JAVA.resolve("com/cafe/controller"), ".java")) {
            Matcher annotations = annotation.matcher(Files.readString(javaFile));
            while (annotations.find()) {
                Matcher paths = path.matcher(annotations.group(1));
                while (paths.find()) {
                    String mapping = paths.group(1);
                    Path previous = owners.putIfAbsent(mapping, javaFile);
                    assertTrue(previous == null,
                            () -> "Servlet mapping trùng " + mapping + ": " + previous + " và " + javaFile);
                }
            }
        }
    }

    @Test
    void controllers_do_not_embed_sql_or_initialize_services_in_fields() throws IOException {
        Pattern sql = Pattern.compile("(?i)\"\\s*(SELECT|INSERT|UPDATE|DELETE|MERGE)\\s+");
        Pattern serviceField = Pattern.compile("private\\s+(?:static\\s+)?final[^;=]*Service\\s+\\w+\\s*=\\s*new\\s+");
        for (Path javaFile : files(MAIN_JAVA.resolve("com/cafe/controller"), ".java")) {
            String source = Files.readString(javaFile);
            assertFalse(sql.matcher(source).find(), () -> "Controller chứa SQL: " + javaFile);
            assertFalse(serviceField.matcher(source).find(),
                    () -> "Controller phải constructor-inject Service: " + javaFile);
        }
    }

    @Test
    void domain_models_do_not_embed_view_formatting_or_css() throws IOException {
        Pattern presentationGetter = Pattern.compile(
                "\\bget[A-Za-z0-9]*(Display|BadgeClass|Badge|Text|Input)\\s*\\(");
        Pattern formattingImport = Pattern.compile(
                "import\\s+(java\\.text\\.(NumberFormat|DecimalFormat)|java\\.time\\.format\\.DateTimeFormatter)");
        for (Path javaFile : files(MAIN_JAVA.resolve("com/cafe/model"), ".java")) {
            String source = Files.readString(javaFile);
            assertFalse(presentationGetter.matcher(source).find(),
                    () -> "Domain model chứa presentation getter: " + javaFile);
            assertFalse(formattingImport.matcher(source).find(),
                    () -> "Domain model tự format dữ liệu cho view: " + javaFile);
            assertFalse(source.contains("badge-"),
                    () -> "Domain model chứa CSS token: " + javaFile);
        }
    }

    @Test
    void branch_manager_presence_and_current_manager_identity_are_not_conflated() throws IOException {
        String authService = Files.readString(
                MAIN_JAVA.resolve("com/cafe/service/auth/AuthService.java"));
        String userDao = Files.readString(
                MAIN_JAVA.resolve("com/cafe/dao/admin/UserDao.java"));
        String userService = Files.readString(
                MAIN_JAVA.resolve("com/cafe/service/admin/UserService.java"));
        String userList = Files.readString(
                WEBAPP.resolve("WEB-INF/views/admin/user-list.jsp"));

        assertTrue(authService.contains("u.getBranchHasManager()"),
                "Đăng nhập phải kiểm tra chi nhánh có Manager, không kiểm tra user hiện tại có phải Manager");
        assertFalse(authService.contains("u.getAssignedBranchManager()"),
                "Cờ xác định Manager hiện tại chỉ phục vụ nghiệp vụ quản trị nhân sự");
        assertTrue(userDao.contains("setBranchHasManager"));
        assertTrue(userDao.contains("setAssignedBranchManager"));
        assertTrue(userList.contains("s.assignedBranchManager"));
        assertFalse(userList.contains("s.branchHasManager"));
        assertTrue(userService.contains(
                "Nhân sự không còn là quản lý chi nhánh. Hãy đổi vai trò trước khi mở khóa tài khoản."));
        assertTrue(userService.contains("u.setStatus(current.getStatus())"),
                "Form sửa nhân sự không được dùng để lách thao tác Khóa/Mở khóa chuyên biệt");
    }

    @Test
    void public_home_is_branch_scoped_and_admin_supports_bulk_product_selection() throws IOException {
        String productDao = Files.readString(
                MAIN_JAVA.resolve("com/cafe/dao/admin/ProductDao.java"));
        String homeServlet = Files.readString(
                MAIN_JAVA.resolve("com/cafe/controller/customer/HomeServlet.java"));
        String publicHome = Files.readString(
                WEBAPP.resolve("WEB-INF/views/customer/home.jsp"));
        String adminHome = Files.readString(
                WEBAPP.resolve("WEB-INF/views/admin/home-editor.jsp"));

        assertTrue(productDao.contains("bm.BranchId = ?"));
        assertTrue(productDao.contains("bm.IsListed = 1"));
        assertTrue(homeServlet.contains("catalog.getPublicHomeBranches()"));
        assertTrue(homeServlet.contains("catalog.getPublicMenu(home.getBranchId())"));
        assertTrue(publicHome.contains("id=\"publicBranch\""));
        assertTrue(adminHome.contains("id=\"showFiltered\""));
        assertTrue(adminHome.contains("id=\"hideFiltered\""));
        assertTrue(adminHome.contains("name=\"branchId\" value=\"${setting.branchId}\""),
                "Lưu danh sách món phải giữ nguyên chi nhánh Admin đang chỉnh sửa");
    }

    @Test
    void business_views_are_reachable_from_java_or_another_jsp() throws IOException {
        List<Path> sources = new ArrayList<>();
        sources.addAll(files(MAIN_JAVA, ".java"));
        sources.addAll(files(WEBAPP, ".jsp"));
        StringBuilder allSources = new StringBuilder();
        for (Path source : sources) allSources.append(Files.readString(source)).append('\n');

        Path views = WEBAPP.resolve("WEB-INF/views");
        for (Path jsp : files(views, ".jsp")) {
            String relative = views.relativize(jsp).toString().replace('\\', '/');
            if (relative.startsWith("error/")) continue;
            assertTrue(allSources.toString().contains(relative)
                            || allSources.toString().contains(jsp.getFileName().toString()),
                    () -> "JSP không có forward/include: " + jsp);
        }
    }

    private static List<Path> files(Path root, String suffix) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(suffix)).toList();
        }
    }
}

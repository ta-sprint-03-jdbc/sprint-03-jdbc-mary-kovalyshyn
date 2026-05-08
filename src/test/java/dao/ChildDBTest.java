package dao;

import model.Child;
import org.junit.jupiter.api.*;
import utils.DBUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Child Database Operations Tests")
class ChildDBTest {

    private ChildDB db;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        new DBUtil().executeFile("init.sql");
        try (Connection conn = DBUtil.getConnection();
        Statement st = conn.createStatement()) {
            st.execute("TRUNCATE TABLE child RESTART IDENTITY CASCADE");
        }
        db = new ChildDB();
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    @DisplayName("Should add a child and return it with an ID")
    void addShouldAddChildAndReturnWithId() throws SQLException {
        String firstName = "John";
        String lastName = "Doe";
        LocalDate birthDate = LocalDate.of(2010, 1, 1);
        Child child = new Child(firstName, lastName, birthDate);

        Child addedChild = db.addChild(child);
        System.out.println("[DEBUG_LOG] Added child ID: " + addedChild.id());

        assertNotNull(addedChild.id(), "Child ID should not be null");
        assertEquals(firstName, addedChild.firstName(), "First name should match");
        assertEquals(lastName, addedChild.lastName(), "Last name should match");
        assertEquals(birthDate, addedChild.birthDate(), "Birth date should match");
    }

    @Test
    @DisplayName("Should add a child with null birth date")
    void addShouldHandleNullBirthDate() throws SQLException {
        Child child = new Child("Jane", "Doe", null);

        Child addedChild = db.addChild(child);

        assertNotNull(addedChild.id());
        assertEquals("Jane", addedChild.firstName());
        assertEquals("Doe", addedChild.lastName());
        assertNull(addedChild.birthDate());

    }

    @Test
    @DisplayName("Should update an existing child")
    void updateShouldUpdateExistingChild() throws SQLException {
        Child child = db.addChild(
                new Child("Tom", "Smith",
                        LocalDate.of(2012, 1, 1)));

        Child updated = new Child(
                child.id(),
                "UpdatedTom",
                "UpdatedSmith",
                LocalDate.of(2011, 2, 2));

        boolean result = db.updateChild(updated);

        assertTrue(result);

        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT * FROM child WHERE id = " + child.id())) {

            assertTrue(rs.next());

            assertEquals("UpdatedTom", rs.getString("first_name"));
            assertEquals("UpdatedSmith", rs.getString("last_name"));
        }

    }


    @Test
    @DisplayName("Should delete an existing child")
    void deleteShouldDeleteExistingChild() throws SQLException {
        Child child = db.addChild(
                new Child("Delete", "Me",
                        LocalDate.of(2010, 1, 1)));

        boolean result = db.deleteChild(child.id());

        assertTrue(result);

        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM child WHERE id = " + child.id())) {

            rs.next();

            assertEquals(0, rs.getInt(1));
        }
    }


    @Test
    @DisplayName("Should return children with at least the specified age")
    void findChildrenWithMinimumAgeShouldReturnChildrenWithMinimumAge() throws SQLException {
        db.addChild(new Child(
                "Older1",
                "Test",
                LocalDate.now().minusYears(10)));

        db.addChild(new Child(
                "Younger",
                "Test",
                LocalDate.now().minusYears(5)));

        db.addChild(new Child(
                "Older2",
                "Test",
                LocalDate.now().minusYears(15)));

        List<Child> result = db.findChildrenWithMinimumAge(10);

        assertEquals(2, result.size());

        assertTrue(result.stream()
                .anyMatch(c -> c.firstName().equals("Older1")));

        assertTrue(result.stream()
                .anyMatch(c -> c.firstName().equals("Older2")));

    }

    @Test
    @DisplayName("Should return children with null birth date")
    void findChildrenWithoutBirthDateShouldReturnChildrenWithNullBirthDate() throws SQLException {
        db.addChild(new Child(
                "WithDate",
                "Test",
                LocalDate.of(2010, 1, 1)));

        db.addChild(new Child(
                "WithoutDate",
                "Test",
                null));

        List<Child> result = db.findChildrenWithoutBirthDate();

        assertFalse(result.isEmpty());

        assertTrue(result.stream()
                .anyMatch(c -> c.firstName().equals("WithoutDate")));

        assertTrue(result.stream()
                .allMatch(c -> c.birthDate() == null));

    }
}


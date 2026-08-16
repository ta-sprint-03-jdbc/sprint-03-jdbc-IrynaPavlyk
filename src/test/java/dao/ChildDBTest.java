package dao;

import model.Child;
import org.junit.jupiter.api.*;
import utils.DBUtil;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Child Database Operations Tests")
class ChildDBTest {

    private ChildDB db;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        new DBUtil().executeFile("init.sql");
        db = new ChildDB();
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("TRUNCATE TABLE child CASCADE");
        }
        db.close();
    }

    @Test
    @DisplayName("Should add a child and return it with an ID")
    void addShouldAddChildAndReturnWithId() throws SQLException {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        LocalDate birthDate = LocalDate.of(2010, 1, 1);
        Child child = new Child(firstName, lastName, birthDate);

        // Act
        Child addedChild = db.addChild(child);
        System.out.println("[DEBUG_LOG] Added child ID: " + addedChild.id());

        // Assert
        assertNotNull(addedChild.id(), "Child ID should not be null");
        assertEquals(firstName, addedChild.firstName(), "First name should match");
        assertEquals(lastName, addedChild.lastName(), "Last name should match");
        assertEquals(birthDate, addedChild.birthDate(), "Birth date should match");
    }

    @Test
    @DisplayName("Should add a child with null birth date")
    void addShouldHandleNullBirthDate() throws SQLException {
        // Arrange
        String firstName = "Johnny";
        String lastName = "Doedoe";
        Child child = new Child(firstName, lastName, null);

        // Act
        Child addedChild = db.addChild(child);
        System.out.println("[DEBUG_LOG] Added child ID: " + addedChild.id());

        // Assert
        assertNotNull(addedChild.id(), "Child ID should not be null");
        assertEquals(firstName, addedChild.firstName(), "First name should match");
        assertEquals(lastName, addedChild.lastName(), "Last name should match");
        assertEquals(null, addedChild.birthDate(), "Birth date is not null");

    }

    @Test
    @DisplayName("Should update an existing child")
    void updateShouldUpdateExistingChild() throws SQLException {
        // Arrange - Add a child first
        String firstName = "John";
        String lastName = "Doe";
        LocalDate birthDate = LocalDate.of(2010, 1, 1);
        Child child = new Child(firstName, lastName, birthDate);
        Child addedChild = db.addChild(child);
        System.out.println("[DEBUG_LOG] Added child ID: " + addedChild.id());

        // Create updated child
        String firstNameUpdated = "John_Updated";
        Child updatedChild = new Child(addedChild.id(), firstNameUpdated, lastName, birthDate);

        // Act
        boolean isUpdated = db.updateChild(updatedChild);

        // Assert
        assertTrue(isUpdated, "Child should be updated!");

        // Verify the update by querying the database
        String query = "SELECT * FROM child WHERE id = ?";
        try (Connection conn = utils.DBUtil.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setLong(1, updatedChild.id());
            try (ResultSet rs = pst.executeQuery()) {
                assertTrue(rs.next(), "Updated child should exist in database");
                assertEquals(updatedChild.firstName(), rs.getString("first_name"), "Category title should be updated in database");
            }
        }

    }


    @Test
    @DisplayName("Should delete an existing child")
    void deleteShouldDeleteExistingChild() throws SQLException {
        // Arrange - Add a child first
        String firstName = "John";
        String lastName = "Doe";
        LocalDate birthDate = LocalDate.of(2010, 1, 1);
        Child child = new Child(firstName, lastName, birthDate);
        Child addedChild = db.addChild(child);
        System.out.println("[DEBUG_LOG] Added child ID: " + addedChild.id());

        // Act
        boolean deletedResult = db.deleteChild(addedChild.id());

        // Assert
        assertTrue(deletedResult, "Child should be deleted!");

        // Verify the deletion by querying the database
        String query = "SELECT * FROM child WHERE id = ?";
        try (Connection conn = utils.DBUtil.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setLong(1, addedChild.id());
            try (ResultSet rs = pst.executeQuery()) {
                assertFalse(rs.next(), "Deleted child should not exist in database!");
            }
        }
    }


    @Test
    @DisplayName("Should return children with null birth date")
    void findChildrenWithoutBirthDateShouldReturnChildrenWithNullBirthDate() throws SQLException {
        // Arrange
        String firstName = "Ivan777";
        String lastName = "Petrenko";

        Child childWithBirthDate =
                db.addChild(new Child(firstName, lastName, LocalDate.now().minusYears(10)));
        Child childWithoutBirthDate =
                db.addChild(new Child(firstName, lastName, null));

        // Act
        List<Child> children = db.findChildrenWithoutBirthDate().stream()
                .filter(c -> firstName.equals(c.firstName()))
                .toList();

        // Assert
        assertEquals(1, children.size(), "Only one child without birth date should be found");

        List<Long> ids = children.stream().map(Child::id).toList();
        assertTrue(ids.contains(childWithoutBirthDate.id()),
                "Child without birth date should be returned");
        assertFalse(ids.contains(childWithBirthDate.id()),
                "Child with birth date should NOT be returned");

        assertTrue(children.stream().allMatch(c -> c.birthDate() == null),
                "All returned children must have null birth date");
    }
}
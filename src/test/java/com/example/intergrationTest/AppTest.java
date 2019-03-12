package com.example.intergrationTest;

import com.example.App;
import com.example.domain.EventConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.*;

import java.io.IOException;
import java.sql.*;

import static junit.framework.TestCase.assertEquals;

public class AppTest {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS event (id VARCHAR(20), duration INTEGER, type VARCHAR(50), host VARCHAR(50), alert BOOLEAN)";
    private ObjectMapper objectMapper;
    private EventConverter eventConverter;
    private App app;

    @BeforeClass
    public static void init() throws SQLException, ClassNotFoundException {
        Class.forName("org.hsqldb.jdbc.JDBCDriver");
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
            connection.commit();
        }
    }

    @AfterClass
    public static void destroy() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE event");
            connection.commit();
            statement.executeUpdate("SHUTDOWN");
            connection.commit();
        }
    }

    @Before
    public void setup() {
        objectMapper = new ObjectMapper();
        eventConverter = new EventConverter();

    }

    @After
    public void cleanup() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM event");
            connection.commit();
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:hsqldb:file:eventdbTest;ifexists=false", "user", "");
    }

    @Test
    public void testRun__ProcessesLogFileAndSavesResultsToDatabase() throws IOException, SQLException {
        String[] args = {"src/test/resources/test.txt"};
        Connection connection = getConnection();

        app = new App(objectMapper, connection, eventConverter);
        app.run(args);

        try (Connection assertConnection = getConnection(); Statement statement = assertConnection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) AS total FROM event");

            assertEquals("Should save 3 records", 3, getSize(resultSet));

            ResultSet result1 = statement.executeQuery("SELECT COUNT(*) AS total FROM event WHERE id='scsmbstgra' AND duration=5 AND type='APPLICATION_LOG' AND host='12345' AND alert=true");
            assertEquals("Should have 1 record", 1, getSize(result1));

            ResultSet result2 = statement.executeQuery("SELECT COUNT(*) AS total FROM event WHERE id='scsmbstgrb' AND duration=3 AND alert=false");
            assertEquals("Should have 1 record", 1, getSize(result2));

            ResultSet result3 = statement.executeQuery("SELECT COUNT(*) AS total FROM event WHERE id='scsmbstgrc' AND duration=8 AND alert=true");
            assertEquals("Should save 1 record", 1, getSize(result3));
        }
    }

    @Test
    public void testRun__DoesntProcessInvalidJsonAndDoesntSavesResultsToDatabase() throws IOException, SQLException {
        String[] args = {"src/test/resources/invalid_json.txt"};
        Connection connection = getConnection();

        app = new App(objectMapper, connection, eventConverter);
        app.run(args);

        try (Connection assertConnection = getConnection(); Statement statement = assertConnection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) AS total FROM event");

            assertEquals("Should save 0 records", 0, getSize(resultSet));
        }
    }

    @Test
    public void testRun__ProcessJsonWithAdditionalFieldsAndSavesResultsToDatabase() throws IOException, SQLException {
        String[] args = {"src/test/resources/test_additional_fields.txt"};
        Connection connection = getConnection();

        app = new App(objectMapper, connection, eventConverter);
        app.run(args);

        try (Connection assertConnection = getConnection(); Statement statement = assertConnection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) AS total FROM event");

            assertEquals("Should save 3 records", 3, getSize(resultSet));

            ResultSet result1 = statement.executeQuery("SELECT COUNT(*) AS total FROM event WHERE id='scsmbstgra' AND duration=5 AND type='APPLICATION_LOG' AND host='12345' AND alert=true");
            assertEquals("Should have 1 record", 1, getSize(result1));

            ResultSet result2 = statement.executeQuery("SELECT COUNT(*) AS total FROM event WHERE id='scsmbstgrb' AND duration=3 AND alert=false");
            assertEquals("Should have 1 record", 1, getSize(result2));

            ResultSet result3 = statement.executeQuery("SELECT COUNT(*) AS total FROM event WHERE id='scsmbstgrc' AND duration=8 AND alert=true");
            assertEquals("Should save 1 record", 1, getSize(result3));
        }
    }

    private int getSize(ResultSet resultSet) throws SQLException {
        resultSet.next();
        return resultSet.getInt("total");
    }
}
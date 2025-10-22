import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class KnightMover {


    /**
     * Represents the game board with its dimensions and obstacles.
     */
    static class Board {
        int width; // Width of the board.
        int height; // Height of the board.
        List<Obstacle> obstacles; // List of obstacles on the board.
    }


    /**
     * Represents an obstacle on the board with its coordinates.
     */
    static class Obstacle {
        int x;
        int y;
    }

    /**
     * Represents a command for the knight, including its type, value (if applicable), and direction (if applicable).
     */
    static class Command {
        String command;
        int value;
        String direction;
    }

    /**
     * Represents the current position and direction of the knight.
     */
    static class Position {
        int x;
        int y;
        String direction;
    }

    /**
     * Represents the result of executing the commands, including the final position (if successful) and the status.
     */
    static class Result {
        Position position;
        String status;
    }

    /**
     * Main method to execute the knight mover program.
     * It fetches board configuration and commands from URLs and then executes the commands.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        try {
            Board board = getBoardConfig("https://storage.googleapis.com/jobrapido-backend-test/board.json");
            List<Command> commands = getCommands("https://storage.googleapis.com/jobrapido-backend-test/commands.json");
            Result result = executeCommands(board, commands);

            Gson gson = new Gson();
            System.out.println(gson.toJson(result));

        } catch (IOException | InterruptedException e) {
            Result errorResult = new Result();
            errorResult.status = "GENERIC_ERROR";
            Gson gson = new Gson();
            System.out.println(gson.toJson(errorResult));
            e.printStackTrace();
        }
    }

    /**
     * Fetches the board configuration from the given URL and parses it into a Board object.
     *
     * @param url The URL to fetch the board configuration from.
     * @return The parsed Board object.
     * @throws IOException          If an I/O error occurs during the HTTP request.
     * @throws InterruptedException If the HTTP request is interrupted.
     */
    public static Board getBoardConfig(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Gson gson = new Gson();
        return gson.fromJson(response.body(), Board.class);
    }

    /**
     * Fetches the list of commands from the given URL and parses them into a List of Command objects.
     *
     * @param url The URL to fetch the commands from.
     * @return The parsed List of Command objects.
     * @throws IOException          If an I/O error occurs during the HTTP request.
     * @throws InterruptedException If the HTTP request is interrupted.
     */

    public static List<Command> getCommands(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
        com.google.gson.JsonArray commandsArray = jsonObject.getAsJsonArray("commands");
        List<Command> commandList = new ArrayList<>();
        for (com.google.gson.JsonElement element : commandsArray) {
            String commandString = element.getAsString();
            String[] parts = commandString.split(" ");
            if(parts[1].toString().contains(",") || parts[1].toString().equals("EAST")|| parts[1].toString().equals("WEST")|| parts[1].toString().equals("NORTH")|| parts[1].toString().equals("SOUTH"))
            {
                commandList.add(createCommand(parts[0], parts[1].toString()));
            }
            else {
                commandList.add(createCommand(parts[0], Integer.parseInt(parts[1])));
            }


        }
        return commandList;
    }
    public static Command createCommand(String command, String details) {
        Command cmd = new Command();
        cmd.command = command;
        if (details != null) {
            try {
                cmd.value = Integer.parseInt(details);
            } catch (NumberFormatException e) {
                cmd.direction = details;
            }
        }
        return cmd;
    }

    public static Command createCommand(String command, int value) {
        Command cmd = new Command();
        cmd.command = command;
        cmd.value = value;
        return cmd;
    }

    /**
     * Executes the list of commands on the given board, updating the knight's position and direction.
     *
     * @param board    The game board.
     * @param commands The list of commands to execute.
     * @return The Result object indicating the final state and status.
     */
    public static Result executeCommands(Board board, List<Command> commands) {
        Result result = new Result();
        Position currentPosition = null;

        for (Command command : commands) {
            switch (command.command) {
                case "START":
                    if (currentPosition != null) {
                        result.status = "GENERIC_ERROR";
                        return result; // Cannot start again
                    }
                    try {
                        String[] parts = command.direction.split(",");
                        int startX = Integer.parseInt(parts[0]);
                        int startY = Integer.parseInt(parts[1]);
                        String startDirection = parts[2];

                        if (startX < 0 || startX >= board.width || startY < 0 || startY >= board.height) {
                            result.status = "INVALID_START_POSITION";
                            return result;
                        }
                        currentPosition = new Position();
                        currentPosition.x = startX;
                        currentPosition.y = startY;
                        currentPosition.direction = startDirection;
                    } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                        result.status = "GENERIC_ERROR";
                        return result;
                    }
                    break;
                case "MOVE":
                    if (currentPosition == null) {
                        result.status = "GENERIC_ERROR";
                        return result; // Cannot move without starting
                    }
                    int moveSteps = command.value;
                    int newX = currentPosition.x;
                    int newY = currentPosition.y;

                    for (int i = 0; i < moveSteps; i++) {
                        int nextX = newX;
                        int nextY = newY;

                        switch (currentPosition.direction) {
                            case "NORTH":
                                nextY++;
                                break;
                            case "SOUTH":
                                nextY--;
                                break;
                            case "EAST":
                                nextX++;
                                break;
                            case "WEST":
                                nextX--;
                                break;
                        }
                        if (nextX < 0 || nextX >= board.width || nextY < 0 || nextY >= board.height) {
                            result.status = "OUT_OF_THE_BOARD";
                            return result;
                        }

                        if (isObstacle(board, nextX, nextY)) {
                            // Stop moving at the obstacle
                            break;
                        }
                        newX = nextX;
                        newY = nextY;
                    }
                    currentPosition.x = newX;
                    currentPosition.y = newY;
                    break;
                case "ROTATE":
                    if (currentPosition == null) {
                        result.status = "GENERIC_ERROR";
                        return result; // Cannot rotate without starting
                    }
                    currentPosition.direction = command.direction;
                    break;
                default:
                    result.status = "GENERIC_ERROR";
                    return result;
            }
        }

        if (currentPosition != null) {
            result.position = currentPosition;
            result.status = "SUCCESS";
        } else {
            result.status = "GENERIC_ERROR"; // Should have started to reach success
        }

        return result;
    }
}
    /**
     * Checks if a given coordinate (x, y) on the board contains an obstacle.
     *
     * @param board The game board.
     * @param x     The X-coordinate to check.
     * @param y     The Y-coordinate to check.
     * @return True if there is an obstacle at the given coordinates, false otherwise.
     */
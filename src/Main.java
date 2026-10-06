import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

/**
 * Starting point of the program. Just opens the Sudoku window.
 */
public class Main {

    public void main(String[] args) {
        // Make the buttons and menus look like normal Windows controls
        // instead of Java's default grey look.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Not a problem - we just keep the default look.
        }

        // Swing windows must be created on the "event dispatch thread",
        // which is what invokeLater does for us.
        SwingUtilities.invokeLater(() -> new SudokuWindow().setVisible(true));
    }
}

/**
 * The four difficulty levels the player can choose in the window.
 *
 * <p>Each level says how many of the 81 cells should be emptied out when a new
 * puzzle is generated. The more cells you remove, the harder the puzzle.
 * Use {@link #getCellsToRemove()} inside {@code SudokuGame.newGame(...)}.
 */
enum Difficulty {

    EASY("Easy", 40),
    MEDIUM("Medium", 48),
    HARD("Hard", 54),
    EXPERT("Expert", 58);


    private final String label;
    private final int cellsToRemove;

    Difficulty(String label, int cellsToRemove) {
        this.label = label;
        this.cellsToRemove = cellsToRemove;
    }

    /** How many cells to blank out when generating a puzzle of this difficulty. */
    public int getCellsToRemove() {
        return cellsToRemove;
    }

    /** The text shown in the difficulty drop-down. */
    @Override
    public String toString() {
        return label;
    }
}

/**
 * THE GAME LOGIC - THIS IS THE CLASS YOU HAVE TO WRITE.
 *
 * <p>The window (SudokuWindow / BoardPanel) is already finished and only talks to
 * the game through the methods below. You do not need to change the window at all:
 * fill in the method bodies here and the game starts working.
 *
 * <p>Everything is stored in three 9x9 arrays:
 * <ul>
 *   <li>{@code board}    - what the player currently sees (0 means an empty cell)</li>
 *   <li>{@code solution} - the completely filled, correct grid</li>
 *   <li>{@code fixed}    - true for the numbers the puzzle gave the player;
 *                          those cells must never be changed</li>
 * </ul>
 *
 * <p>Rows and columns are counted from 0 to 8. Cell (0, 0) is the top-left corner
 * and cell (8, 8) is the bottom-right one.
 *
 * <p>Suggested order to implement things in:
 * <ol>
 *   <li>getCell, isFixed, countEmptyCells - tiny methods, just read the arrays</li>
 *   <li>isValidMove - the Sudoku rule check (row, column, 3x3 box)</li>
 *   <li>setCell, clearCell, reset - changing the board</li>
 *   <li>newGame - build a full solution, then remove cells</li>
 *   <li>isConflict, isSolved, getSolutionCell - the finishing touches</li>
 * </ol>
 */
class SudokuGame {

    /** Width and height of the grid. */
    public static final int SIZE = 9;

    /** Width and height of one small 3x3 box. */
    public static final int BOX_SIZE = 3;

    /** The value stored in a cell that has no number in it yet. */
    public static final int EMPTY = 0;

    // ------------------------------------------------------------------
    // The data of one game
    // ------------------------------------------------------------------

    /** What the player sees right now. 0 = empty cell. */
    private final int[][] board = new int[SIZE][SIZE];

    /** The full correct grid, used for hints and for checking the answer. */
    private final int[][] solution = new int[SIZE][SIZE];

    /** true = this number came with the puzzle and cannot be edited. */
    private final boolean[][] fixed = new boolean[SIZE][SIZE];

    /** The difficulty of the puzzle that is currently being played. */
    private Difficulty difficulty = Difficulty.EASY;

    // ------------------------------------------------------------------
    // Starting and restarting a game
    // ------------------------------------------------------------------

    /**
     * Creates a brand new puzzle of the given difficulty.
     *
     * <p>What this method has to do:
     * <ol>
     *   <li>Fill {@code solution} with a valid, complete Sudoku grid.
     *       A simple way: start with an empty grid and fill it cell by cell,
     *       trying the numbers 1-9 in a random order and going back a step
     *       whenever no number fits (this is called backtracking).</li>
     *   <li>Copy {@code solution} into {@code board}.</li>
     *   <li>Pick {@code difficulty.getCellsToRemove()} random cells and set them
     *       to {@link #EMPTY} in {@code board}.</li>
     *   <li>Set {@code fixed[row][col]} to true for every cell that still has a
     *       number in it, and false for the emptied ones.</li>
     *   <li>Remember the difficulty in the {@code difficulty} field.</li>
     * </ol>
     *
     * @param difficulty the level chosen in the drop-down; never null
     */
    public void newGame(Difficulty difficulty) {
        this.difficulty = difficulty;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                solution[r][c] = EMPTY;
            }
        }
        generateSolution(0, 0);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                board[r][c] = solution[r][c];
                fixed[r][c] = true;
            }
        }
        int cellsToRemove = difficulty.getCellsToRemove();
        Random rand = new Random();

        while (cellsToRemove > 0) {
            int r = rand.nextInt(SIZE);
            int c = rand.nextInt(SIZE);

            if (board[r][c] != EMPTY) {
                board[r][c] = EMPTY;
                fixed[r][c] = false;
                cellsToRemove--;
            }
        }
    }

    /**
     * Removes every number the player typed in, but keeps the puzzle itself.
     * In other words: set each cell that is not {@code fixed} back to {@link #EMPTY}.
     * The window calls this from the "Restart" button.
     */
    public void reset() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!fixed[r][c]) {
                    board[r][c] = EMPTY;
                }
            }
        }
    }

    /** @return the difficulty of the puzzle that is being played. */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    // ------------------------------------------------------------------
    // Reading the board
    // ------------------------------------------------------------------

    /**
     * The number the player currently sees in one cell.
     *
     * @param row 0-8, counted from the top
     * @param col 0-8, counted from the left
     * @return the number 1-9, or {@link #EMPTY} (0) if the cell is empty
     */
    public int getCell(int row, int col) {
        return board[row][col];
    }

    /**
     * Was this number part of the puzzle from the start?
     * The window draws these in black and refuses to change them.
     *
     * @return true if the cell cannot be edited by the player
     */
    public boolean isFixed(int row, int col) {
        return fixed[row][col];
    }

    /**
     * How many cells are still empty. The window shows this in the status bar.
     *
     * @return a number between 0 and 81
     */
    public int countEmptyCells() {
        int count = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == EMPTY) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * The correct number for one cell, taken from the solution.
     * The window uses this for the "Hint" and "Show solution" buttons.
     *
     * @return the number 1-9 that belongs in this cell
     */
    public int getSolutionCell(int row, int col) {
        return solution[row][col];
    }

    // ------------------------------------------------------------------
    // Changing the board
    // ------------------------------------------------------------------

    /**
     * Writes a number into a cell.
     *
     * <p>Important: this method should NOT refuse a move just because it breaks a
     * Sudoku rule. The player is allowed to make mistakes - the window paints wrong
     * numbers in red using {@link #isConflict(int, int)}. Only refuse the move when
     * it is impossible:
     * <ul>
     *   <li>row or col outside 0-8, or value outside 1-9</li>
     *   <li>the cell is {@code fixed}</li>
     * </ul>
     *
     * @param value the number 1-9 the player pressed
     * @return true if the number was written, false if the move was refused
     */
    public boolean setCell(int row, int col, int value) {
        if (row<0|| row >=SIZE || col<0|| col >=SIZE || value<1|| value>9 || fixed[row][col]) return false;
        board[row][col] = value;
        return true;
    }

    /**
     * Empties one cell again (the "Erase" button and the Delete key).
     * A {@code fixed} cell must not be erased.
     *
     * @return true if the cell was emptied, false if it was refused
     */
    public boolean clearCell(int row, int col) {
        if (row<0|| row >=SIZE || col<0|| col >=SIZE || fixed[row][col]) return false;
        board[row][col] = 0;
        return true;
    }

    // ------------------------------------------------------------------
    // The rules
    // ------------------------------------------------------------------

    /**
     * Would placing this number here follow the three Sudoku rules?
     * A number is allowed when it does not already appear
     * <ul>
     *   <li>anywhere else in the same row,</li>
     *   <li>anywhere else in the same column,</li>
     *   <li>anywhere else in the same 3x3 box.</li>
     * </ul>
     *
     * <p>The cell you are asking about does not count as a duplicate of itself.
     * The top-left corner of the box around (row, col) is
     * {@code (row / BOX_SIZE) * BOX_SIZE} and {@code (col / BOX_SIZE) * BOX_SIZE}.
     *
     * @param value the number 1-9 to test
     * @return true if the move follows the rules
     */
    public boolean isValidMove(int row, int col, int value) {
        for (int i=0; i<SIZE; i++) {
            if (board[row][i] == value || board[i][col] == value || board[(row/BOX_SIZE)*BOX_SIZE+i/BOX_SIZE][(col/BOX_SIZE)*BOX_SIZE+i%BOX_SIZE] == value) return false;
        }
        int boxRow = (row/BOX_SIZE)*BOX_SIZE;
        int boxCol = (col/BOX_SIZE)*BOX_SIZE;
        for (int i=0; i<BOX_SIZE; i++) {
            for (int j = 0; j < BOX_SIZE; j++) {
                int rIdx = boxRow + i;
                int cIdx = boxCol + j;
                if ((rIdx != row && cIdx != col) && board[rIdx][cIdx] == value) return false;
            }
        }
        return true;
    }

    /**
     * Is the number currently in this cell breaking a rule?
     * The window paints these cells with a red background.
     *
     * <p>An empty cell is never a conflict. For a filled cell you can reuse
     * {@link #isValidMove(int, int, int)} with the number that is already there.
     *
     * @return true if this cell clashes with another cell
     */
    public boolean isConflict(int row, int col) {
        int value = board[row][col];
        if (value == 0) return false;
        return !isValidMove(row, col, value);
    }

    /**
     * Has the player won? That means every cell is filled in and no cell
     * breaks a rule. (Comparing the whole board with the solution also works.)
     *
     * @return true if the puzzle is finished correctly
     */
    public boolean isSolved() {
        if (countEmptyCells() > 0) return false;

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (isConflict(row, col)) return false;
            }
        }

        return true;
    }
}

private boolean generateSolution(int row, int col){
    return false;
}

/**
 * The main window: the grid in the middle, the controls on the right and a
 * status bar at the bottom.
 *
 * <p>This class only builds and refreshes the interface. All the rules of Sudoku
 * live in {@link SudokuGame}.
 */
class SudokuWindow extends JFrame {

    private final SudokuGame game = new SudokuGame();
    private final BoardPanel boardPanel;

    private final JComboBox<Difficulty> difficultyBox = new JComboBox<>(Difficulty.values());
    private final JLabel emptyCellsLabel = new JLabel();
    private final JLabel timeLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" ");

    /** Counts the seconds since the current puzzle was started. */
    private final Timer clock;
    private int secondsPlayed;

    /** Makes sure the "you won" message is only shown once per puzzle. */
    private boolean alreadyCongratulated;

    public SudokuWindow() {
        super("Sudoku");

        boardPanel = new BoardPanel(game, new BoardPanel.Callback() {
            @Override
            public void boardChanged() {
                refreshLabels();
                checkForWin();
            }

            @Override
            public void showMessage(String text) {
                statusLabel.setText(text);
            }
        });

        clock = new Timer(1000, e -> {
            secondsPlayed++;
            updateTimeLabel();
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel boardHolder = new JPanel(new BorderLayout());
        boardHolder.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 6));
        boardHolder.setBackground(new Color(0xF7, 0xF8, 0xFA));
        boardHolder.add(boardPanel, BorderLayout.CENTER);

        add(boardHolder, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
        add(buildStatusBar(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(null);

        startNewGame();
    }

    // ------------------------------------------------------------------
    // Building the interface
    // ------------------------------------------------------------------

    private JPanel buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 12));
        side.setPreferredSize(new Dimension(210, 0));

        side.add(sectionTitle("Difficulty"));
        difficultyBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        difficultyBox.setAlignmentX(LEFT_ALIGNMENT);
        difficultyBox.addActionListener(e -> startNewGame());
        side.add(difficultyBox);

        side.add(Box.createVerticalStrut(16));
        side.add(sectionTitle("Numbers"));
        side.add(buildNumberPad());

        JButton eraseButton = wideButton("Erase (Del)");
        eraseButton.addActionListener(e -> {
            boardPanel.eraseSelected();
            boardPanel.requestFocusInWindow();
        });
        side.add(Box.createVerticalStrut(6));
        side.add(eraseButton);

        side.add(Box.createVerticalStrut(16));
        side.add(sectionTitle("Game"));
        side.add(action("New puzzle", this::startNewGame));
        side.add(Box.createVerticalStrut(6));
        side.add(action("Restart this one", this::restartPuzzle));
        side.add(Box.createVerticalStrut(6));
        side.add(action("Hint", this::giveHint));
        side.add(Box.createVerticalStrut(6));
        side.add(action("Check board", this::checkBoard));
        side.add(Box.createVerticalStrut(6));
        side.add(action("Show solution", this::showSolution));

        side.add(Box.createVerticalGlue());

        emptyCellsLabel.setAlignmentX(LEFT_ALIGNMENT);
        timeLabel.setAlignmentX(LEFT_ALIGNMENT);
        side.add(emptyCellsLabel);
        side.add(timeLabel);

        return side;
    }

    private JPanel buildNumberPad() {
        JPanel pad = new JPanel(new GridLayout(3, 3, 6, 6));
        pad.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        pad.setAlignmentX(LEFT_ALIGNMENT);

        for (int value = 1; value <= 9; value++) {
            int number = value;
            JButton button = new JButton(String.valueOf(number));
            button.setFont(button.getFont().deriveFont(Font.BOLD, 16f));
            button.setFocusable(false);
            button.addActionListener(e -> {
                boardPanel.placeNumber(number);
                boardPanel.requestFocusInWindow();
            });
            pad.add(button);
        }
        return pad;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD0, 0xD4, 0xDA)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        statusLabel.setForeground(new Color(0x44, 0x4C, 0x56));
        bar.add(statusLabel, BorderLayout.WEST);

        JLabel help = new JLabel("Click a cell - type 1-9 - arrows to move");
        help.setForeground(new Color(0x8A, 0x91, 0x9B));
        help.setHorizontalAlignment(SwingConstants.RIGHT);
        bar.add(help, BorderLayout.EAST);

        return bar;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setForeground(new Color(0x55, 0x5D, 0x68));
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return label;
    }

    private JButton wideButton(String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return button;
    }

    /** A full-width button that runs the given method when clicked. */
    private JButton action(String text, Runnable whatToDo) {
        JButton button = wideButton(text);
        button.addActionListener(e -> {
            whatToDo.run();
            boardPanel.requestFocusInWindow();
        });
        return button;
    }

    // ------------------------------------------------------------------
    // What the buttons do
    // ------------------------------------------------------------------

    private void startNewGame() {
        Difficulty chosen = (Difficulty) difficultyBox.getSelectedItem();
        game.newGame(chosen == null ? Difficulty.EASY : chosen);

        secondsPlayed = 0;
        alreadyCongratulated = false;
        startClock();
        refreshLabels();
        boardPanel.repaint();
        boardPanel.requestFocusInWindow();

        if (boardLooksEmpty()) {
            statusLabel.setText("Board is empty - implement newGame() in SudokuGame.java.");
        } else {
            statusLabel.setText("New " + chosen + " puzzle. Good luck!");
        }
    }

    private void restartPuzzle() {
        game.reset();
        secondsPlayed = 0;
        alreadyCongratulated = false;
        startClock();
        refreshLabels();
        boardPanel.repaint();
        statusLabel.setText("Puzzle restarted.");
    }

    private void giveHint() {
        int row = boardPanel.getSelectedRow();
        int col = boardPanel.getSelectedCol();

        if (row < 0 || col < 0) {
            statusLabel.setText("Select an empty cell, then press Hint.");
            return;
        }
        if (game.isFixed(row, col)) {
            statusLabel.setText("That cell already came with the puzzle.");
            return;
        }

        int correct = game.getSolutionCell(row, col);
        if (correct == SudokuGame.EMPTY) {
            statusLabel.setText("No solution stored - implement newGame() and getSolutionCell().");
            return;
        }

        game.setCell(row, col, correct);
        refreshLabels();
        boardPanel.repaint();
        statusLabel.setText("Hint: this cell is a " + correct + ".");
        checkForWin();
    }

    private void checkBoard() {
        int empty = game.countEmptyCells();
        int mistakes = countConflicts();

        if (mistakes > 0) {
            statusLabel.setText(mistakes + " cell(s) break a rule - they are marked red.");
        } else if (empty > 0) {
            statusLabel.setText("No mistakes so far. " + empty + " cell(s) still empty.");
        } else {
            statusLabel.setText("The board is full and correct!");
        }
        boardPanel.repaint();
        checkForWin();
    }

    private void showSolution() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Fill in the whole solution and end this game?",
                "Show solution", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        for (int row = 0; row < SudokuGame.SIZE; row++) {
            for (int col = 0; col < SudokuGame.SIZE; col++) {
                if (!game.isFixed(row, col)) {
                    game.setCell(row, col, game.getSolutionCell(row, col));
                }
            }
        }

        stopClock();
        alreadyCongratulated = true;
        refreshLabels();
        boardPanel.repaint();
        statusLabel.setText("Solution shown. Start a new puzzle when you are ready.");
    }

    // ------------------------------------------------------------------
    // Keeping the labels up to date
    // ------------------------------------------------------------------

    private void refreshLabels() {
        emptyCellsLabel.setText("Empty cells: " + game.countEmptyCells());
        updateTimeLabel();
    }

    private void updateTimeLabel() {
        timeLabel.setText(String.format("Time: %02d:%02d", secondsPlayed / 60, secondsPlayed % 60));
    }

    private void startClock() {
        updateTimeLabel();
        clock.start();
    }

    private void stopClock() {
        clock.stop();
    }

    /** Shows the congratulations dialog the first moment the puzzle is finished. */
    private void checkForWin() {
        if (alreadyCongratulated || !game.isSolved()) {
            return;
        }
        alreadyCongratulated = true;
        stopClock();

        String time = String.format("%d:%02d", secondsPlayed / 60, secondsPlayed % 60);
        statusLabel.setText("Solved in " + time + "!");
        JOptionPane.showMessageDialog(this,
                "Solved on " + game.getDifficulty() + " in " + time + ".",
                "Well done!", JOptionPane.INFORMATION_MESSAGE);
    }

    private int countConflicts() {
        int total = 0;
        for (int row = 0; row < SudokuGame.SIZE; row++) {
            for (int col = 0; col < SudokuGame.SIZE; col++) {
                if (game.getCell(row, col) != SudokuGame.EMPTY && game.isConflict(row, col)) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * True when there is not a single number on the board. That normally means
     * {@code newGame()} has not been written yet, so we can say so instead of
     * leaving the player staring at an empty grid.
     */
    private boolean boardLooksEmpty() {
        for (int row = 0; row < SudokuGame.SIZE; row++) {
            for (int col = 0; col < SudokuGame.SIZE; col++) {
                if (game.getCell(row, col) != SudokuGame.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }
}

/**
 * The 9x9 grid itself: draws the board and turns mouse clicks and key presses
 * into calls on {@link SudokuGame}.
 *
 * <p>This class keeps no copy of the numbers. Every time it repaints it asks the
 * game what to show, so the drawing can never get out of sync with the logic.
 */
class BoardPanel extends JPanel {

    /** How the panel tells the window that something happened. */
    public interface Callback {
        /** A number was placed or erased - the window should refresh its labels. */
        void boardChanged();

        /** Something worth saying out loud, shown in the status bar. */
        void showMessage(String text);
    }

    private static final Color COLOR_CELL = Color.WHITE;
    private static final Color COLOR_SELECTED = new Color(0xBB, 0xDE, 0xFB);
    private static final Color COLOR_PEER = new Color(0xEF, 0xF3, 0xF8);
    private static final Color COLOR_SAME_NUMBER = new Color(0xDC, 0xEB, 0xFA);
    private static final Color COLOR_CONFLICT_CELL = new Color(0xFF, 0xEB, 0xEE);
    private static final Color COLOR_LINE_THIN = new Color(0xC8, 0xCD, 0xD4);
    private static final Color COLOR_LINE_THICK = new Color(0x2F, 0x36, 0x40);
    private static final Color COLOR_DIGIT_FIXED = new Color(0x21, 0x25, 0x2B);
    private static final Color COLOR_DIGIT_PLAYER = new Color(0x15, 0x65, 0xC0);
    private static final Color COLOR_DIGIT_CONFLICT = new Color(0xD3, 0x2F, 0x2F);

    private final SudokuGame game;
    private final Callback callback;

    /** The cell the player has selected, or -1 when nothing is selected. */
    private int selectedRow = 0;
    private int selectedCol = 0;

    public BoardPanel(SudokuGame game, Callback callback) {
        this.game = game;
        this.callback = callback;

        setPreferredSize(new Dimension(504, 504));
        setMinimumSize(new Dimension(288, 288));
        setBackground(new Color(0xF7, 0xF8, 0xFA));
        setFocusable(true);
        setToolTipText("Click a cell, then type 1-9. Delete erases.");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                selectCellAt(e.getX(), e.getY());
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e);
            }
        });
    }

    // ------------------------------------------------------------------
    // Selection and input
    // ------------------------------------------------------------------

    /** @return the selected row, or -1 if no cell is selected. */
    public int getSelectedRow() {
        return selectedRow;
    }

    /** @return the selected column, or -1 if no cell is selected. */
    public int getSelectedCol() {
        return selectedCol;
    }

    /** Puts the number the player chose into the selected cell. */
    public void placeNumber(int value) {
        if (selectedRow < 0 || selectedCol < 0) {
            callback.showMessage("Select a cell first.");
            return;
        }
        if (game.isFixed(selectedRow, selectedCol)) {
            callback.showMessage("That number came with the puzzle - it cannot be changed.");
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        boolean wouldFollowRules = game.isValidMove(selectedRow, selectedCol, value);

        if (!game.setCell(selectedRow, selectedCol, value)) {
            callback.showMessage("That move was refused by SudokuGame.setCell().");
            repaint();
            return;
        }

        callback.showMessage(wouldFollowRules
                ? "Placed " + value + "."
                : value + " clashes with another cell - see the red squares.");
        repaint();
        callback.boardChanged();
    }

    /** Empties the selected cell. */
    public void eraseSelected() {
        if (selectedRow < 0 || selectedCol < 0) {
            callback.showMessage("Select a cell first.");
            return;
        }
        if (game.isFixed(selectedRow, selectedCol)) {
            callback.showMessage("That number came with the puzzle - it cannot be erased.");
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        game.clearCell(selectedRow, selectedCol);
        callback.showMessage("Cell cleared.");
        repaint();
        callback.boardChanged();
    }

    /** Moves the selection with the arrow keys, staying inside the grid. */
    private void moveSelection(int rowStep, int colStep) {
        if (selectedRow < 0 || selectedCol < 0) {
            selectedRow = 0;
            selectedCol = 0;
        } else {
            selectedRow = Math.clamp(selectedRow + rowStep, 0, SudokuGame.SIZE - 1);
            selectedCol = Math.clamp(selectedCol + colStep, 0, SudokuGame.SIZE - 1);
        }
        repaint();
    }

    private void handleKey(KeyEvent e) {
        int code = e.getKeyCode();

        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
            placeNumber(code - KeyEvent.VK_0);
        } else if (code >= KeyEvent.VK_NUMPAD1 && code <= KeyEvent.VK_NUMPAD9) {
            placeNumber(code - KeyEvent.VK_NUMPAD0);
        } else if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE
                || code == KeyEvent.VK_0 || code == KeyEvent.VK_NUMPAD0
                || code == KeyEvent.VK_SPACE) {
            eraseSelected();
        } else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
            moveSelection(-1, 0);
        } else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
            moveSelection(1, 0);
        } else if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
            moveSelection(0, -1);
        } else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
            moveSelection(0, 1);
        }
    }

    private void selectCellAt(int mouseX, int mouseY) {
        int cell = cellSize();
        if (cell <= 0) {
            return;
        }
        int col = (mouseX - originX()) / cell;
        int row = (mouseY - originY()) / cell;

        boolean insideGrid = mouseX >= originX() && mouseY >= originY()
                && row >= 0 && row < SudokuGame.SIZE
                && col >= 0 && col < SudokuGame.SIZE;

        selectedRow = insideGrid ? row : -1;
        selectedCol = insideGrid ? col : -1;
        repaint();
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    /** Side length of one cell in pixels - the grid always stays square. */
    private int cellSize() {
        return Math.min(getWidth(), getHeight()) / SudokuGame.SIZE;
    }

    /** Left edge of the grid, so it sits in the middle of the panel. */
    private int originX() {
        return (getWidth() - cellSize() * SudokuGame.SIZE) / 2;
    }

    /** Top edge of the grid. */
    private int originY() {
        return (getHeight() - cellSize() * SudokuGame.SIZE) / 2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cell = cellSize();
        int x0 = originX();
        int y0 = originY();
        int gridSize = cell * SudokuGame.SIZE;

        g2.setColor(COLOR_CELL);
        g2.fillRect(x0, y0, gridSize, gridSize);

        paintCellBackgrounds(g2, x0, y0, cell);
        paintGridLines(g2, x0, y0, cell, gridSize);
        paintNumbers(g2, x0, y0, cell);

        g2.dispose();
    }

    private void paintCellBackgrounds(Graphics2D g2, int x0, int y0, int cell) {
        int selectedValue = (selectedRow >= 0 && selectedCol >= 0)
                ? game.getCell(selectedRow, selectedCol)
                : SudokuGame.EMPTY;

        for (int row = 0; row < SudokuGame.SIZE; row++) {
            for (int col = 0; col < SudokuGame.SIZE; col++) {
                Color background = null;

                if (row == selectedRow && col == selectedCol) {
                    background = COLOR_SELECTED;
                } else if (game.getCell(row, col) != SudokuGame.EMPTY
                        && game.isConflict(row, col)) {
                    background = COLOR_CONFLICT_CELL;
                } else if (selectedValue != SudokuGame.EMPTY
                        && game.getCell(row, col) == selectedValue) {
                    background = COLOR_SAME_NUMBER;
                } else if (isPeerOfSelection(row, col)) {
                    background = COLOR_PEER;
                }

                if (background != null) {
                    g2.setColor(background);
                    g2.fillRect(x0 + col * cell, y0 + row * cell, cell, cell);
                }
            }
        }
    }

    /** Is this cell in the same row, column or 3x3 box as the selected one? */
    private boolean isPeerOfSelection(int row, int col) {
        if (selectedRow < 0 || selectedCol < 0) {
            return false;
        }
        boolean sameBox = row / SudokuGame.BOX_SIZE == selectedRow / SudokuGame.BOX_SIZE
                && col / SudokuGame.BOX_SIZE == selectedCol / SudokuGame.BOX_SIZE;
        return row == selectedRow || col == selectedCol || sameBox;
    }

    private void paintGridLines(Graphics2D g2, int x0, int y0, int cell, int gridSize) {
        for (int i = 0; i <= SudokuGame.SIZE; i++) {
            boolean boxBorder = i % SudokuGame.BOX_SIZE == 0;
            g2.setColor(boxBorder ? COLOR_LINE_THICK : COLOR_LINE_THIN);
            g2.setStroke(new BasicStroke(boxBorder ? 2.5f : 1f));

            int offset = i * cell;
            g2.drawLine(x0 + offset, y0, x0 + offset, y0 + gridSize);
            g2.drawLine(x0, y0 + offset, x0 + gridSize, y0 + offset);
        }
    }

    private void paintNumbers(Graphics2D g2, int x0, int y0, int cell) {
        Font fixedFont = getFont().deriveFont(Font.BOLD, cell * 0.58f);
        Font playerFont = getFont().deriveFont(Font.PLAIN, cell * 0.58f);

        for (int row = 0; row < SudokuGame.SIZE; row++) {
            for (int col = 0; col < SudokuGame.SIZE; col++) {
                int value = game.getCell(row, col);
                if (value == SudokuGame.EMPTY) {
                    continue;
                }

                boolean isFixed = game.isFixed(row, col);
                g2.setFont(isFixed ? fixedFont : playerFont);
                if (isFixed) {
                    g2.setColor(COLOR_DIGIT_FIXED);
                } else {
                    g2.setColor(game.isConflict(row, col)
                            ? COLOR_DIGIT_CONFLICT
                            : COLOR_DIGIT_PLAYER);
                }

                // Centre the digit inside its cell.
                String text = String.valueOf(value);
                FontMetrics metrics = g2.getFontMetrics();
                int textX = x0 + col * cell + (cell - metrics.stringWidth(text)) / 2;
                int textY = y0 + row * cell + (cell - metrics.getHeight()) / 2
                        + metrics.getAscent();
                g2.drawString(text, textX, textY);
            }
        }
    }
}

void main() {
}
export type CellType = "BASE" | "PATH" | "HOME" | "START" | "CENTER" | "EMPTY";
export type Color = "RED" | "BLUE" | "YELLOW" | "GREEN";

export interface Cell {
  id: string;
  row: number;
  col: number;
  type: CellType;
  color?: Color;
  span?: number;
}

export const generateBoard = (): Cell[] => {
  const cells: Cell[] = [];

  cells.push({
    id: "base-red",
    row: 1,
    col: 1,
    type: "BASE",
    color: "RED",
    span: 4,
  });
  cells.push({
    id: "base-blue",
    row: 1,
    col: 12,
    type: "BASE",
    color: "BLUE",
    span: 4,
  });
  cells.push({
    id: "base-yellow",
    row: 12,
    col: 12,
    type: "BASE",
    color: "YELLOW",
    span: 4,
  });
  cells.push({
    id: "base-green",
    row: 12,
    col: 1,
    type: "BASE",
    color: "GREEN",
    span: 4,
  });

  cells.push({ id: "center-main", row: 7, col: 7, type: "CENTER", span: 3 });

  for (let r = 1; r <= 15; r++) {
    for (let c = 1; c <= 15; c++) {
      const isBaseRed = r <= 4 && c <= 4;
      const isBaseBlue = r <= 4 && c >= 12;
      const isBaseYellow = r >= 12 && c >= 12;
      const isBaseGreen = r >= 12 && c <= 4;

      const isCenter = r >= 7 && r <= 9 && c >= 7 && c <= 9;

      if (
        !isBaseRed &&
        !isBaseBlue &&
        !isBaseYellow &&
        !isBaseGreen &&
        !isCenter
      ) {
        let type: CellType = "EMPTY";
        let color: Color | undefined;

        const isHorizontalPath = r >= 7 && r <= 9;
        const isVerticalPath = c >= 7 && c <= 9;

        if (isHorizontalPath || isVerticalPath) {
          type = "PATH";

          if (c === 8 && r > 1 && r < 7) {
            type = "HOME";
            color = "BLUE";
          }
          if (c === 8 && r > 9 && r < 15) {
            type = "HOME";
            color = "GREEN";
          }
          if (r === 8 && c > 1 && c < 7) {
            type = "HOME";
            color = "RED";
          }
          if (r === 8 && c > 9 && c < 15) {
            type = "HOME";
            color = "YELLOW";
          }

          if (r === 7 && c === 2) {
            type = "START";
            color = "RED";
          }
          if (r === 2 && c === 9) {
            type = "START";
            color = "BLUE";
          }
          if (r === 9 && c === 14) {
            type = "START";
            color = "YELLOW";
          }
          if (r === 14 && c === 7) {
            type = "START";
            color = "GREEN";
          }
        }

        cells.push({ id: `cell-${r}-${c}`, row: r, col: c, type, color });
      }
    }
  }
  return cells;
};

export const BOARD_CELLS = generateBoard();

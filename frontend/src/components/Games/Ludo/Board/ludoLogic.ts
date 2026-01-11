import { Color } from "./constants";

const PATH_MAP: [number, number][] = [
  [7, 2],
  [7, 3],
  [7, 4],
  [7, 5],
  [7, 6],
  [6, 7],
  [5, 7],
  [4, 7],
  [3, 7],
  [2, 7],
  [1, 7],
  [1, 8],
  [1, 9],
  [2, 9],
  [3, 9],
  [4, 9],
  [5, 9],
  [6, 9],
  [7, 10],
  [7, 11],
  [7, 12],
  [7, 13],
  [7, 14],
  [7, 15],
  [8, 15],
  [9, 15],
  [9, 14],
  [9, 13],
  [9, 12],
  [9, 11],
  [9, 10],
  [10, 9],
  [11, 9],
  [12, 9],
  [13, 9],
  [14, 9],
  [15, 9],
  [15, 8],
  [15, 7],
  [14, 7],
  [13, 7],
  [12, 7],
  [11, 7],
  [10, 7],
  [9, 6],
  [9, 5],
  [9, 4],
  [9, 3],
  [9, 2],
  [9, 1],
  [8, 1],
  [7, 1],
];

export const getPawnCoords = (
  position: number,
  color: Color,
  pawnIndex: number
) => {
  if (position === -1) {
    const bases = {
      RED: { r: [2, 2, 3, 3], c: [2, 3, 2, 3] },
      BLUE: { r: [2, 2, 3, 3], c: [13, 14, 13, 14] },
      YELLOW: { r: [13, 13, 14, 14], c: [13, 14, 13, 14] },
      GREEN: { r: [13, 13, 14, 14], c: [2, 3, 2, 3] },
    };
    return { row: bases[color].r[pawnIndex], col: bases[color].c[pawnIndex] };
  }

  if (position >= 100) {
    const step = position - 100;
    const homeStretch = {
      RED: { row: 8, col: 2 + step },
      BLUE: { row: 2 + step, col: 8 },
      YELLOW: { row: 8, col: 14 - step },
      GREEN: { row: 14 - step, col: 8 },
    };
    return homeStretch[color];
  }

  const offsets = {
    RED: 0,
    BLUE: 13,
    YELLOW: 26,
    GREEN: 39,
  };

  const playerOffset = offsets[color];
  const totalLength = PATH_MAP.length;

  const targetIndex = (position + playerOffset) % totalLength;
  const [row, col] = PATH_MAP[targetIndex];

  return { row, col };
};

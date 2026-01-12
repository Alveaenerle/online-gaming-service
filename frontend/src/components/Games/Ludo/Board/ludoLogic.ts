import { Color } from "./constants";

export const PATH_MAP: [number, number][] = [
  // 🔴 RED start → w prawo
  [7, 2],
  [7, 3],
  [7, 4],
  [7, 5],
  [7, 6],

  // ↑ do góry
  [6, 7],
  [5, 7],
  [4, 7],
  [3, 7],
  [2, 7],

  // → w prawo (BLUE entry)
  [2, 8],
  [2, 9],
  [3, 9],
  [4, 9],
  [5, 9],
  [6, 9],

  // ↓ w dół
  [7, 10],
  [7, 11],
  [7, 12],
  [7, 13],
  [7, 14],

  // ↓ (YELLOW side)
  [8, 14],
  [9, 14],
  [9, 13],
  [9, 12],
  [9, 11],
  [9, 10],

  // → w prawo do GREEN side
  [10, 9],
  [11, 9],
  [12, 9],
  [13, 9],
  [14, 9],

  // ← w lewo
  [14, 8],
  [14, 7],
  [13, 7],
  [12, 7],
  [11, 7],
  [10, 7],

  // ↑ do RED side
  [9, 6],
  [9, 5],
  [9, 4],
  [9, 3],
  [9, 2],

  [8, 2],
];

export const getPawnCoords = (
  position: number,
  stepsMoved: number,
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
    const coords = {
      row: bases[color].r[pawnIndex % 4],
      col: bases[color].c[pawnIndex % 4],
    };
    return coords;
  }
  const offsets = { RED: 0, BLUE: 13, YELLOW: 26, GREEN: 39 };
  if (position === -2 && stepsMoved > 43) {
    const step = stepsMoved - 43;
    if (step >= 5) return { row: 8, col: 8 };

    const homeStretch = {
      RED: { row: 8, col: 2 + step },
      BLUE: { row: 2 + step, col: 8 },
      YELLOW: { row: 8, col: 14 - step },
      GREEN: { row: 14 - step, col: 8 },
    };
    return homeStretch[color];
  }

  const targetIndex = position % PATH_MAP.length;
  const [row, col] = PATH_MAP[targetIndex];

  return { row, col };
};

export const getPathCoords = (
  startPos: number,
  steps: number,
  color: Color,
  pawnIndex: number,
  stepsMoved: number
) => {
  console.group(
    `%c[LOGIC-PATH] Calculating path for ${color} pawn`,
    "color: #ff00ff"
  );
  console.log(`Start Pos: ${startPos}, Steps: ${steps}`);

  const path = [];
  let currentPos = startPos;

  for (let i = 1; i <= steps; i++) {
    const oldPos = currentPos;

    if (currentPos === -1) {
      currentPos = 0;
    } else {
      currentPos++;
      stepsMoved++;
      if (stepsMoved > 43) {
        break;
      }
    }

    const coords = getPawnCoords(currentPos, stepsMoved, color, pawnIndex);
    path.push(coords);

    console.log(
      `Step ${i}: ${oldPos} -> ${currentPos} | Coords: [${coords.row}, ${coords.col}]`
    );
  }

  console.log("%cFull Path Array:", "color: #00ff00", path);
  console.groupEnd();

  return path;
};

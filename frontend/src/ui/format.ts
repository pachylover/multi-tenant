export function bytesToMb(bytes: number) {
  return bytes / (1024 * 1024);
}

export function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n));
}


import type { SignupDateValue } from "../types/signup";

export function getDaysInMonth(year: number, month: number) {
  return new Date(year, month, 0).getDate();
}

export function formatSignupDate({ year, month, day }: SignupDateValue) {
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

export function parseSignupDate(value: string): SignupDateValue {
  const [year, month, day] = value.split("-").map(Number);
  const currentDate = new Date();

  if (year && month && day) {
    return { year, month, day };
  }

  return {
    year: currentDate.getFullYear(),
    month: currentDate.getMonth() + 1,
    day: currentDate.getDate(),
  };
}


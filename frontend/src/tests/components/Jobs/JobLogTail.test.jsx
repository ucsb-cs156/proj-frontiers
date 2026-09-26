import { act, render, screen } from "@testing-library/react";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import JobLogTail from "main/components/Jobs/JobLogTail";

const axiosMock = new AxiosMockAdapter(axios);
const tailUrl = "/api/jobs/course/logs/tail";
const testId = "JobLogTail";

const line = (id, message) => ({ id, jobId: 12, message });

const advance = (ms) =>
  act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });

describe("JobLogTail tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    delete HTMLElement.prototype.scrollTop;
    delete HTMLElement.prototype.scrollHeight;
  });

  test("says it is loading, and that there are no lines yet, before the first answer", () => {
    axiosMock.onGet(tailUrl).reply(200, { status: "running", lines: [] });

    render(<JobLogTail courseId={7} jobId={12} />);

    expect(screen.getByTestId(`${testId}-status`)).toHaveTextContent(
      "Job 12 — Loading the log...",
    );
    expect(
      screen.queryByTestId(`${testId}-status-badge`),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-log`)).toHaveTextContent(
      "No log lines yet.",
    );
  });

  test("shows the log lines, one per line, in monospace white on black", async () => {
    axiosMock.onGet(tailUrl).reply(200, {
      status: "running",
      lines: [line(1, "Creating repo lab01-cgaucho"), line(2, "  done")],
    });

    render(<JobLogTail courseId={7} jobId={12} />);
    await advance(0);

    const log = screen.getByTestId(`${testId}-log`);
    expect(log.tagName).toBe("PRE");
    expect(log.textContent).toBe("Creating repo lab01-cgaucho\n  done");
    // a long log scrolls inside its own box instead of stretching the page
    expect(log.style.backgroundColor).toBe("black");
    expect(log.style.color).toBe("white");
    expect(log.style.fontFamily).toBe("monospace");
    expect(log.style.maxHeight).toBe("60vh");
    expect(log.style.overflowY).toBe("auto");
    expect(log.style.whiteSpace).toBe("pre-wrap");
    expect(log.style.padding).toBe("0.75rem");
  });

  test("while the job is running it shows its status and that it checks every 3 seconds", async () => {
    axiosMock.onGet(tailUrl).reply(200, { status: "running", lines: [] });

    render(<JobLogTail courseId={7} jobId={12} />);
    await advance(0);

    expect(screen.getByTestId(`${testId}-status-badge`)).toHaveTextContent(
      "running",
    );
    expect(screen.getByTestId(`${testId}-status-text`)).toHaveTextContent(
      "Checking for new lines every 3 seconds.",
    );
    expect(screen.getByTestId(`${testId}-status`)).toHaveTextContent(
      "Job 12 running — Checking for new lines every 3 seconds.",
    );
  });

  test("follows the log as it is written, and says when the job is over", async () => {
    axiosMock
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [line(1, "one")] })
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [line(2, "two")] })
      .onGet(tailUrl)
      .replyOnce(200, { status: "complete", lines: [line(3, "three")] });

    render(<JobLogTail courseId={7} jobId={12} />);
    await advance(0);
    expect(screen.getByTestId(`${testId}-log`).textContent).toBe("one");

    await advance(3000);
    expect(screen.getByTestId(`${testId}-log`).textContent).toBe("one\ntwo");

    await advance(3000);
    expect(screen.getByTestId(`${testId}-log`).textContent).toBe(
      "one\ntwo\nthree",
    );
    expect(screen.getByTestId(`${testId}-status-badge`)).toHaveTextContent(
      "complete",
    );
    expect(screen.getByTestId(`${testId}-status-text`)).toHaveTextContent(
      "The job is over, so the log will not change any more.",
    );

    await advance(60000);
    expect(axiosMock.history.get.length).toBe(3);
  });

  test("says when the log could not be loaded", async () => {
    axiosMock.onGet(tailUrl).reply(404);

    render(<JobLogTail courseId={7} jobId={12} />);
    await advance(0);

    expect(screen.getByTestId(`${testId}-status-text`)).toHaveTextContent(
      "The log could not be loaded.",
    );
    expect(screen.getByTestId(`${testId}-log`)).toHaveTextContent(
      "No log lines yet.",
    );
  });

  test("scrolls to the end of the log when lines are added", async () => {
    const scrollTops = [];
    Object.defineProperty(HTMLElement.prototype, "scrollTop", {
      configurable: true,
      get: () => 0,
      set: (value) => {
        scrollTops.push(value);
      },
    });
    Object.defineProperty(HTMLElement.prototype, "scrollHeight", {
      configurable: true,
      get: () => 500,
    });
    axiosMock
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [line(1, "one")] })
      .onGet(tailUrl)
      .replyOnce(200, { status: "complete", lines: [line(2, "two")] });

    render(<JobLogTail courseId={7} jobId={12} />);
    await advance(0);
    const afterFirst = scrollTops.length;
    expect(scrollTops[afterFirst - 1]).toBe(500);

    await advance(3000);
    expect(scrollTops.length).toBeGreaterThan(afterFirst);
    expect(scrollTops[scrollTops.length - 1]).toBe(500);
  });

  test("uses a custom testIdPrefix", async () => {
    axiosMock.onGet(tailUrl).reply(200, { status: "complete", lines: [] });

    render(<JobLogTail courseId={7} jobId={12} testIdPrefix="Custom" />);
    await advance(0);

    expect(screen.getByTestId("Custom")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-status")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-log")).toBeInTheDocument();
  });

  test("asks for the job of the course it is given", () => {
    axiosMock.onGet(tailUrl).reply(200, { status: "complete", lines: [] });

    render(<JobLogTail courseId={3} jobId={40} />);

    expect(axiosMock.history.get[0].params).toEqual({
      courseId: 3,
      jobId: 40,
      afterId: 0,
    });
  });
});

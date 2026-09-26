import { act, renderHook } from "@testing-library/react";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { useJobLogTail } from "main/utils/useJobLogTail";

const axiosMock = new AxiosMockAdapter(axios);
const tailUrl = "/api/jobs/course/logs/tail";
const tailRequests = () =>
  axiosMock.history.get.filter((request) => request.url === tailUrl);

const line = (id, message) => ({ id, jobId: 12, message });

// let the pending promises settle, and the timers that are due run
const advance = (ms) =>
  act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });

describe("useJobLogTail tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  test("starts with nothing, and asks for the whole log of the job of the course", async () => {
    axiosMock
      .onGet(tailUrl)
      .reply(200, { status: "running", lines: [line(1, "first")] });

    const { result } = renderHook(() => useJobLogTail(7, 12));

    expect(result.current).toEqual({ lines: [], status: null, error: null });
    expect(tailRequests().length).toBe(1);
    expect(tailRequests()[0].params).toEqual({
      courseId: 7,
      jobId: 12,
      afterId: 0,
    });

    await advance(0);

    expect(result.current).toEqual({
      lines: ["first"],
      status: "running",
      error: null,
    });
  });

  test("what the very first render sees is nothing yet", async () => {
    axiosMock
      .onGet(tailUrl)
      .reply(200, { status: "running", lines: [line(1, "first")] });
    const seen = [];
    const { unmount } = renderHook(() => {
      const value = useJobLogTail(7, 12);
      seen.push(value);
      return value;
    });

    expect(seen[0]).toEqual({ lines: [], status: null, error: null });
    await advance(0);
    unmount();
  });

  test("polls again every 3 seconds, asking only for the lines after the last one it has", async () => {
    axiosMock
      .onGet(tailUrl)
      .replyOnce(200, {
        status: "running",
        lines: [line(5, "a"), line(6, "b")],
      })
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [line(9, "c")] })
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [line(12, "d")] });

    const { result } = renderHook(() => useJobLogTail(7, 12));
    await advance(0);
    expect(result.current.lines).toEqual(["a", "b"]);

    await advance(2999);
    expect(tailRequests().length).toBe(1);

    await advance(1);
    expect(tailRequests().length).toBe(2);
    expect(tailRequests()[1].params).toEqual({
      courseId: 7,
      jobId: 12,
      afterId: 6,
    });
    // the new lines are added to the ones it has
    expect(result.current.lines).toEqual(["a", "b", "c"]);

    await advance(2999);
    expect(tailRequests().length).toBe(2);
    await advance(1);
    expect(tailRequests().length).toBe(3);
    expect(tailRequests()[2].params.afterId).toBe(9);
    expect(result.current.lines).toEqual(["a", "b", "c", "d"]);
  });

  test("a poll with no new lines keeps what it has, and asks after the same line again", async () => {
    axiosMock
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [line(5, "a")] })
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [] })
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [] });

    const { result } = renderHook(() => useJobLogTail(7, 12));
    await advance(0);
    await advance(3000);
    await advance(3000);

    expect(tailRequests().length).toBe(3);
    expect(tailRequests()[1].params.afterId).toBe(5);
    expect(tailRequests()[2].params.afterId).toBe(5);
    expect(result.current.lines).toEqual(["a"]);
    expect(result.current.status).toBe("running");
  });

  test("the status is the latest one", async () => {
    axiosMock
      .onGet(tailUrl)
      .replyOnce(200, { status: "queued", lines: [] })
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [] });

    const { result } = renderHook(() => useJobLogTail(7, 12));
    await advance(0);
    expect(result.current.status).toBe("queued");

    await advance(3000);
    expect(result.current.status).toBe("running");
  });

  test("stops polling once the job is over, after taking its last lines", async () => {
    axiosMock
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [line(1, "a")] })
      .onGet(tailUrl)
      .replyOnce(200, { status: "complete", lines: [line(2, "done")] });

    const { result } = renderHook(() => useJobLogTail(7, 12));
    await advance(0);
    await advance(3000);

    expect(result.current.lines).toEqual(["a", "done"]);
    expect(result.current.status).toBe("complete");
    expect(tailRequests().length).toBe(2);

    await advance(60000);
    expect(tailRequests().length).toBe(2);
  });

  test("a job that is over already is asked about once", async () => {
    for (const status of ["error", "cancelled", "interrupted"]) {
      axiosMock.reset();
      axiosMock.resetHistory();
      axiosMock.onGet(tailUrl).reply(200, { status, lines: [line(1, "boom")] });

      const { result, unmount } = renderHook(() => useJobLogTail(7, 12));
      await advance(0);
      await advance(60000);

      expect(result.current.status).toBe(status);
      expect(tailRequests().length).toBe(1);
      unmount();
    }
  });

  test("stops polling, and says why, when a request fails", async () => {
    axiosMock
      .onGet(tailUrl)
      .replyOnce(200, { status: "running", lines: [line(1, "a")] })
      .onGet(tailUrl)
      .replyOnce(404);

    const { result } = renderHook(() => useJobLogTail(7, 12));
    await advance(0);
    await advance(3000);

    expect(result.current.error).not.toBeNull();
    expect(result.current.error.response.status).toBe(404);
    // what it had is kept
    expect(result.current.lines).toEqual(["a"]);
    expect(tailRequests().length).toBe(2);

    await advance(60000);
    expect(tailRequests().length).toBe(2);
  });

  test("stops polling when the component goes away", async () => {
    axiosMock
      .onGet(tailUrl)
      .reply(200, { status: "running", lines: [line(1, "a")] });

    const { unmount } = renderHook(() => useJobLogTail(7, 12));
    await advance(0);
    expect(tailRequests().length).toBe(1);

    unmount();
    await advance(60000);

    expect(tailRequests().length).toBe(1);
  });

  test("an answer that arrives after the component has gone away is dropped, and does not start polling again", async () => {
    let answer;
    axiosMock.onGet(tailUrl).reply(
      () =>
        new Promise((resolve) => {
          answer = resolve;
        }),
    );

    const { result, unmount } = renderHook(() => useJobLogTail(7, 12));
    await advance(0);
    expect(tailRequests().length).toBe(1);
    expect(answer).toBeDefined();

    unmount();
    await act(async () => {
      answer([200, { status: "running", lines: [line(1, "late")] }]);
    });
    await advance(60000);

    expect(result.current.lines).toEqual([]);
    expect(tailRequests().length).toBe(1);
  });

  test("an error that arrives after the component has gone away is dropped", async () => {
    let fail;
    axiosMock.onGet(tailUrl).reply(
      () =>
        new Promise((resolve, reject) => {
          fail = reject;
        }),
    );

    const { result, unmount } = renderHook(() => useJobLogTail(7, 12));
    await advance(0);

    unmount();
    await act(async () => {
      fail(new Error("too late"));
    });
    await advance(60000);

    expect(result.current.error).toBeNull();
    expect(tailRequests().length).toBe(1);
  });

  test("a job with another id starts again from the beginning, and the old one is no longer followed", async () => {
    axiosMock
      .onGet(tailUrl, { params: { courseId: 7, jobId: 12, afterId: 0 } })
      .reply(200, { status: "running", lines: [line(3, "old job")] });
    axiosMock
      .onGet(tailUrl, { params: { courseId: 7, jobId: 13, afterId: 0 } })
      .reply(200, { status: "running", lines: [line(1, "new job")] });

    const { result, rerender } = renderHook(
      ({ jobId }) => useJobLogTail(7, jobId),
      { initialProps: { jobId: 12 } },
    );
    await advance(0);
    expect(result.current.lines).toEqual(["old job"]);

    rerender({ jobId: 13 });
    expect(result.current).toEqual({ lines: [], status: null, error: null });
    await advance(0);
    expect(result.current.lines).toEqual(["new job"]);

    await advance(3000);
    const jobIds = tailRequests().map((request) => request.params.jobId);
    // the old job was asked about once, then only the new one
    expect(jobIds).toEqual([12, 13, 13]);
  });

  test("a failed request is forgotten when the job changes", async () => {
    axiosMock
      .onGet(tailUrl, { params: { courseId: 7, jobId: 12, afterId: 0 } })
      .reply(500);
    axiosMock
      .onGet(tailUrl, { params: { courseId: 7, jobId: 13, afterId: 0 } })
      .reply(200, { status: "complete", lines: [line(1, "fine")] });

    const { result, rerender } = renderHook(
      ({ jobId }) => useJobLogTail(7, jobId),
      { initialProps: { jobId: 12 } },
    );
    await advance(0);
    expect(result.current.error).not.toBeNull();

    rerender({ jobId: 13 });
    await advance(0);

    expect(result.current.error).toBeNull();
    expect(result.current.lines).toEqual(["fine"]);
  });
});

import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import JobLogModal from "main/components/Jobs/JobLogModal";

const axiosMock = new AxiosMockAdapter(axios);
const tailUrl = "/api/jobs/course/logs/tail";
const testId = "JobLogModal";

const advance = (ms) =>
  act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });

const renderModal = (props = {}) =>
  render(
    <JobLogModal
      show={true}
      onHide={vi.fn()}
      courseId={7}
      jobId={12}
      {...props}
    />,
  );

describe("JobLogModal tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    vi.useFakeTimers();
    axiosMock.onGet(tailUrl).reply(200, {
      status: "running",
      lines: [{ id: 1, jobId: 12, message: "hello" }],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  test("shows the log of the job in a large, centered modal titled with the job number", async () => {
    renderModal();
    await advance(0);

    expect(screen.getByText("Job 12 Log")).toBeInTheDocument();
    expect(screen.getByTestId(testId)).toHaveClass(
      "modal-dialog modal-dialog-centered modal-lg",
    );
    expect(screen.getByTestId(`${testId}-tail-log`).textContent).toBe("hello");
    expect(axiosMock.history.get[0].params).toEqual({
      courseId: 7,
      jobId: 12,
      afterId: 0,
    });
  });

  test("keeps following the log while it is open", async () => {
    renderModal();
    await advance(0);
    await advance(3000);
    await advance(3000);

    expect(axiosMock.history.get.length).toBe(3);
  });

  test("the close button asks to hide the modal", async () => {
    const onHide = vi.fn();
    renderModal({ onHide });
    await advance(0);

    fireEvent.click(screen.getByLabelText("Close"));

    expect(onHide).toHaveBeenCalledTimes(1);
  });

  test("asks for nothing while it is not shown", async () => {
    renderModal({ show: false });
    await advance(60000);

    expect(axiosMock.history.get.length).toBe(0);
    expect(screen.queryByText("Job 12 Log")).not.toBeInTheDocument();
  });

  test("stops following the log when it is hidden: the log goes away, and with it its polling", async () => {
    // the modal fades out, which needs real timers to finish
    vi.useRealTimers();
    const { rerender } = renderModal();
    await waitFor(() => expect(axiosMock.history.get.length).toBe(1));
    expect(screen.getByTestId(`${testId}-tail`)).toBeInTheDocument();

    rerender(
      <JobLogModal show={false} onHide={vi.fn()} courseId={7} jobId={12} />,
    );

    await waitFor(() =>
      expect(screen.queryByTestId(`${testId}-tail`)).not.toBeInTheDocument(),
    );
    expect(screen.queryByText("Job 12 Log")).not.toBeInTheDocument();
    expect(axiosMock.history.get.length).toBe(1);
  });

  test("uses a custom testIdPrefix", async () => {
    renderModal({ testIdPrefix: "Custom" });
    await advance(0);

    expect(screen.getByTestId("Custom")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-tail")).toBeInTheDocument();
  });
});

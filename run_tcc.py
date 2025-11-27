import os
import subprocess
import sys
import platform

# Paths to the libraries (may be differ)
# Windows: java -Djava.library.path="C:\Program Files\IBM\ILOG\CPLEX_Studio2212\opl\bin\x64_win64" -jar target\tcc-1.0.jar <inputType> <input-file> <output-file>
# Linux: /opt/ibm/ILOG/CPLEX_Studio2212/opl/bin/x86-64_linux
CPLEX_PATH = "C:/Program Files/IBM/ILOG/CPLEX_Studio2212/opl/bin/x64_win64"

USE_CPLEX = True

MAX_RUNNING_TIME = "3605s"

class TimeoutError(Exception):
    """Custom exception for timeout errors."""
    pass

def compile_code(source_folder):
    print(f"Compiling code in {source_folder}...")

    # Run Maven compile without changing directory
    result = subprocess.run(
        ["mvn.cmd", "clean", "package"],
        capture_output=True,
        text=True,
        cwd=source_folder
    )

    if result.returncode != 0:
        print("Maven compilation failed:")
        print(result.stderr)
        return False

    print("Maven compilation successful.")
    return True


def run_benchmark(source_folder, input_folder, output_folder, problem_type):
    # Make sure output folder exists
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    # Set the library path (if needed)
    if USE_CPLEX:
        libraries = CPLEX_PATH

    if platform.system() == "Darwin":
        timeout_command = "gtimeout"
    else:
        timeout_command = "timeout"

    # Get the path to the JAR file
    jar_path = os.path.join(source_folder, "target", "tcc-1.0.jar")

    for filename in os.listdir(input_folder):
        if filename.endswith(".dat"):
            print(f"Running {filename} as type {problem_type}")
            input_file = os.path.join(input_folder, filename)
            output_file = os.path.join(output_folder, f"{os.path.splitext(filename)[0]}.txt")

            # Main Java command
            if platform.system() == "Windows":
                cmd = [
                    "java",
                    "-Xmx16g",
                    f"-Djava.library.path={libraries}",
                    "-jar",
                    jar_path,
                    problem_type,
                    input_file,
                    output_file
                ]
            else:
                # Linux / Mac
                timeout_command = "timeout" if platform.system() != "Darwin" else "gtimeout"
                cmd = [
                    timeout_command,
                    MAX_RUNNING_TIME,
                    "java",
                    f"-Djava.library.path={libraries}",
                    "-Xmx16g",
                    "-jar",
                    jar_path,
                    problem_type,
                    input_file,
                    output_file
                ]

            if USE_CPLEX:
                cmd.insert(3, f"-Djava.library.path={libraries}")

            result = subprocess.run(
                cmd,
                stderr=subprocess.PIPE,
                text=True,
                cwd=source_folder  # Set working directory directly
            )

            # Check for timeout (return code 124 is the standard timeout exit code)
            if result.returncode == 124:
                error_msg = f"Execution timed out after {MAX_RUNNING_TIME} for {input_file}"
                print(error_msg)
                raise TimeoutError(error_msg)
            elif result.returncode != 0:
                print(f"Execution failed for {input_file}:")
                print(result.stderr)
                raise RuntimeError(f"Execution failed for {input_file}: {result.stderr}")


if __name__ == "__main__":
    if len(sys.argv) != 5:
        print("Usage: python run_tcc.py <source_folder> <input_folder> <output_folder> <type>")
        print("       type: NEARP or NEARPTP")
        sys.exit(1)

    source_folder = sys.argv[1]
    input_folder = sys.argv[2]
    output_folder = sys.argv[3]
    problem_type = sys.argv[4]

    # Convert to absolute paths
    source_folder = os.path.abspath(source_folder)
    input_folder = os.path.abspath(input_folder)
    output_folder = os.path.abspath(output_folder)

    if compile_code(source_folder):
        run_benchmark(source_folder, input_folder, output_folder, problem_type)
